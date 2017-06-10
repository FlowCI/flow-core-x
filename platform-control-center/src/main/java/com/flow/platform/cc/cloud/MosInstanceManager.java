package com.flow.platform.cc.cloud;

import com.flow.platform.cc.util.DateUtil;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.mos.Instance;
import com.flow.platform.util.mos.MosClient;
import com.flow.platform.util.mos.MosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Meituan cloud cloud manager
 * <p>
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
@Component(value = "mosInstanceManager")
public class MosInstanceManager implements InstanceManager {

    private final static String INSTANCE_NAME_PATTERN = "%s.cloud.mos";
    private final static int MAX_NUM_OF_INSTANCE = 10; // max num of instance control by platform
    private final static int INSTANCE_MAX_ALIVE_DURATION = 600; // max instance alive time in seconds

    @Value("${mos.image}")
    private String imageName;

    @Value("${mos.instance_name_pattern}")
    private String instanceNamePattern;

    @Value("${mos.instance_name_start_rule}")
    private String instanceNameStartWith;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private MosClient mosClient;

    // running mos instance
    private final Map<String, Instance> mosRunningList = new ConcurrentHashMap<>();

    // failed mos instances or instance needs to clean
    private final Map<String, Instance> mosCleanupList = new ConcurrentHashMap<>();

    @Override
    public Instance find(String name) {
        return mosRunningList.get(name);
    }

    @Override
    public Instance find(AgentPath agentPath) {
        String instanceName = String.format(INSTANCE_NAME_PATTERN, agentPath.getName());
        return find(instanceName);
    }

    @Override
    public Collection<Instance> runningInstance() {
        return mosRunningList.values();
    }

    @Override
    public Collection<Instance> failureInstance() {
        return mosCleanupList.values();
    }

    @Override
    public List<String> batchStartInstance(int numOfInstance) {
        // check total num of instance
        int totalInstance = mosCleanupList.size() + mosRunningList.size();
        if (totalInstance + numOfInstance > MAX_NUM_OF_INSTANCE) {
            numOfInstance = MAX_NUM_OF_INSTANCE - totalInstance;
        }

        List<String> expectNameList = new ArrayList<>(numOfInstance);
        for (int i = 0; i < numOfInstance; i ++) {
            String instanceName = createUniqueInstanceName();
            taskExecutor.execute(new StartMosInstanceWorker(mosClient, imageName, instanceName));
            expectNameList.add(instanceName);
        }
        return expectNameList;
    }

    @Override
    public void addToCleanList(Instance instance) {
        mosRunningList.remove(instance.getName());
        mosCleanupList.putIfAbsent(instance.getName(), instance);
    }

    @Override
    public void cleanFromProvider(long maxAliveDuration, String status) {
        List<Instance> instances = mosClient.listInstance();

        ZonedDateTime timeForNow = DateUtil.fromDateForUTC(new Date());

        for (Instance instance : instances) {

            // check instance is controlled by platform
            if (!instance.getName().startsWith(instanceNameStartWith)) {
                continue;
            }

            // find alive duration
            Date createdAt = instance.getCreatedAt();
            ZonedDateTime mosUtcTime = DateUtil.fromDateForUTC(createdAt);
            long aliveInSeconds = ChronoUnit.SECONDS.between(mosUtcTime, timeForNow);

            System.out.println(String.format("Instance %s alive %s", instance.getName(), aliveInSeconds));

            // delete instance if instance status is ready (closed) and alive duration > max alive duration
            if (aliveInSeconds >= maxAliveDuration && instance.getStatus().equals(status)) {
                mosClient.deleteInstance(instance.getInstanceId());
            }
        }
    }

    /**
     * Delete all failure and running instance
     */
    @Override
    public void cleanAll() {
        cleanInstance(mosCleanupList);
        cleanInstance(mosRunningList);
    }

    /**
     * Delete failed created instance every 2 mins
     */
    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 60 * 1000)
    public void cleanInstanceTask() {
        cleanInstance(mosCleanupList);

        // clean up mos instance when status is shutdown
        cleanFromProvider(INSTANCE_MAX_ALIVE_DURATION, Instance.STATUS_READY);
    }

    private void cleanInstance(Map<String, Instance> instanceMap) {
        Iterator<Map.Entry<String, Instance>> iterator = instanceMap.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, Instance> entry = iterator.next();
            Instance mosInstance = entry.getValue();
            mosClient.deleteInstance(mosInstance.getInstanceId());
            iterator.remove();
        }
    }

    private String createUniqueInstanceName() {
        return String.format(instanceNamePattern, UUID.randomUUID());
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
                    System.out.println(instance);
                    mosRunningList.put(instanceName, instance);
                } else {
                    mosCleanupList.put(instanceName, instance);
                }
            } catch (Throwable e) {
                // TODO: should add logging

                if (e instanceof MosException) {
                    MosException mosException = (MosException) e;

                    // should deal with failed created instance
                    if (mosException.getInstance() != null) {
                        mosCleanupList.put(instanceName, mosException.getInstance());
                    }
                }
            }
        }
    }
}
