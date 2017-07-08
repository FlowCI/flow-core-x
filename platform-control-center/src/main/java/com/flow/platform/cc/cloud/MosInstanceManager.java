package com.flow.platform.cc.cloud;

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.domain.Instance;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.Logger;
import com.flow.platform.util.mos.MosInstance;
import com.flow.platform.util.mos.MosClient;
import com.flow.platform.util.mos.MosException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

    private final static Logger LOGGER = new Logger(MosInstanceManager.class);

    @Value("${mos.instance_max_num}")
    private Integer instanceMaxNum;

    @Value("${mos.instance_max_alive}")
    private Integer instanceMaxAlive; // max alive duration in seconds

    @Value("${mos.instance_name_pattern}")
    private String instanceNamePattern;

    @Value("${mos.instance_name_start_rule}")
    private String instanceNameStartWith;

    @Value("${mos.agent_name_pattern}")
    private String agentNamePattern;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private MosClient mosClient;

    @Autowired
    private TaskConfig taskConfig;

    // failed mos instances or instance needs to clean
    private final Map<String, Instance> mosCleanupList = new ConcurrentHashMap<>();

    @Override
    public String instanceName() {
        return String.format(instanceNamePattern, UUID.randomUUID());
    }

    @Override
    public Instance find(String name) {
        return mosClient.find(name, true);
    }

    @Override
    public Instance find(AgentPath agentPath) {
        String instanceName = String.format(agentNamePattern, agentPath.getName());
        return find(instanceName);
    }

    @Override
    public Collection<Instance> instances() {
        List<MosInstance> instances = mosClient.listInstance();
        instances.removeIf(instance -> !instance.getName().startsWith(instanceNameStartWith));
        return Lists.newArrayList(instances);
    }

    @Override
    public List<String> batchStartInstance(final Zone zone) {
        // check total num of instance
        int totalInstance = instances().size();
        int numOfInstanceToStart = zone.getNumOfStart();

        LOGGER.trace("batchStartInstance",
            "Total: %s, clean list: %s", totalInstance, mosCleanupList.size());

        // ensure num of instance not over the max
        if (totalInstance + numOfInstanceToStart> instanceMaxNum) {
            numOfInstanceToStart = instanceMaxNum - totalInstance;
        }

        LOGGER.traceMarker(
            "batchStartInstance", "Num of instance should start %s", numOfInstanceToStart);

        List<String> expectNameList = new ArrayList<>(numOfInstanceToStart);
        for (int i = 0; i < numOfInstanceToStart; i++) {
            String instanceName = instanceName();

            taskExecutor.execute(
                new StartMosInstanceWorker(mosClient, zone.getImageName(), instanceName));

            expectNameList.add(instanceName);
        }

        return expectNameList;
    }

    @Override
    public void addToCleanList(Instance instance) {
        mosCleanupList.putIfAbsent(instance.getName(), instance);
    }

    @Override
    public void cleanFromProvider(long maxAliveDuration, String status) {
        for (Instance instance : instances()) {

            // find alive duration
            Date createdAt = instance.getCreatedAt();
            ZonedDateTime mosUtcTime = DateUtil.fromDateForUTC(createdAt);
            long aliveInSeconds = ChronoUnit.SECONDS.between(mosUtcTime, DateUtil.utcNow());
            LOGGER.trace("Instance %s alive %s seconds", instance.getName(), aliveInSeconds);

            // delete instance if instance status is ready (closed) and alive duration > max alive duration
            if (aliveInSeconds >= maxAliveDuration && instance.getStatus().equals(status)) {
                mosClient.deleteInstance(instance.getId());
                LOGGER.trace("Clean instance which over max alive time: %s", instance);
            }
        }
    }

    /**
     * Delete all failure and running instance
     */
    @Override
    public void cleanAll() {
        cleanInstance(instances());
    }

    /**
     * Delete failed created instance
     * Delete the instance which reach the max alive duration
     */
    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 300 * 1000)
    public void cleanInstanceTask() {
        if (!taskConfig.isEnableMosCleanTask()) {
            return;
        }

        LOGGER.traceMarker("cleanInstanceTask", "start");
        cleanInstance(mosCleanupList.values());

        // clean up mos instance when status is shutdown
        cleanFromProvider(instanceMaxAlive, MosInstance.STATUS_READY);
        LOGGER.traceMarker("cleanInstanceTask", "end");
    }

    private void cleanInstance(Collection<Instance> instances) {
        Iterator<Instance> iterator = instances.iterator();
        while (iterator.hasNext()) {
            Instance mosInstance = iterator.next();

            mosClient.deleteInstance(mosInstance.getId());
            iterator.remove();

            LOGGER.trace("Clean instance from cleanup list: %s", mosInstance);
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
            final int timeToWait = 30; // seconds

            try {
                MosInstance instance = mosClient.createInstance(imageName, instanceName);

                boolean hasRunningStatus = mosClient.instanceStatusSync(
                    instance.getId(), MosInstance.STATUS_RUNNING, timeToWait * 1000);

                boolean hasIpBound = !Strings.isNullOrEmpty(instance.getIp());

                if (hasRunningStatus && hasIpBound) {
                    LOGGER.trace("Instance status is running %s", instance);
                } else {
                    LOGGER.trace(
                        "Instance status not correct after %s seconds %s", timeToWait, instance);
                    mosCleanupList.put(instanceName, instance);
                }
            } catch (Throwable e) {
                LOGGER.error("Unable to create mos instance", e);

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
