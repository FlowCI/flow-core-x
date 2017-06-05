package com.flow.platform.cc.pool;

import com.flow.platform.util.mos.Instance;
import com.flow.platform.util.mos.MosClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Meituan cloud pool manager
 * <p>
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
@Component
public class MosPoolManager implements PoolManager {

    @Value("${mos.key}")
    private String mosKey;

    @Value("${mos.secret}")
    private String mosSecret;

    @Value("${mos.image}")
    private String mosImage;

    @Autowired
    private Executor taskExecutor;

    private MosClient mosClient;

    // running mos instance
    private final Queue<Instance> mosRunningQueue = new ConcurrentLinkedQueue<>();

    // failed mos instances
    private final Queue<Instance> mosFailureQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init () throws Throwable {
        mosClient = new MosClient(mosKey, mosSecret);
    }

    @Override
    public List<String> batchStartInstance(int numOfInstance) {
        List<String> expectNameList = new ArrayList<>(numOfInstance);
        for (int i = 0; i < numOfInstance; i ++) {
            String instanceName = String.format("flow-platform-mos-%s", UUID.randomUUID());
            taskExecutor.execute(new StartMosInstanceWorker(mosClient, mosImage, instanceName));
            expectNameList.add(instanceName);
        }
        return expectNameList;
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void deleteFailureInstance() {
        for (Instance instance : mosFailureQueue) {
            mosClient.deleteInstance(instance.getInstanceId());
        }
    }

    /**
     * Thread to start zone instance
     */
    private class StartMosInstanceWorker implements Runnable {

        private final MosClient mosClient;
        private final String imageName;
        private final String instanceName;

        private StartMosInstanceWorker(MosClient mosClient, String imageName, String instanceName) {
            this.mosClient = mosClient;
            this.imageName = imageName;
            this.instanceName = instanceName;
        }

        @Override
        public void run() {
            Instance instance = null;
            try {
                instance = mosClient.createInstance(imageName, instanceName);

                // wait instance status to running with 30 seconds timeout
                if (mosClient.instanceStatusSync(instance.getInstanceId(), Instance.STATUS_RUNNING, 30 * 1000)) {
                    mosRunningQueue.add(instance);
                } else {
                    mosFailureQueue.add(instance);
                }
            } catch (Throwable e) {
                // TODO: should add logging
                if (instance != null) {
                    mosFailureQueue.add(instance);
                }
            }
        }
    }
}
