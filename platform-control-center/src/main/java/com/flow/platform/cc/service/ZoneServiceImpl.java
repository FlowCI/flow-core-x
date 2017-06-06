package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.*;
import com.google.common.collect.Lists;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service(value = "zoneService")
public class ZoneServiceImpl extends ZkServiceBase implements ZoneService {

    private static final int MIN_NUM_OF_IDLE_AGENT = 2;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private SpringContextUtil springContextUtil;

    private final Map<Zone, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    @PostConstruct
    private void init() {
        // init root node and watch children event
        String rootPath = zkHelper.buildZkPath(null, null).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes
        for (Zone zone : zkHelper.getZones()) {
            createZone(zone);
        }
    }

    @Override
    public String createZone(Zone zone) {
        final String zonePath = zkHelper.buildZkPath(zone.getName(), null).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, agentConfig.toJson());
        } else{
            ZkNodeHelper.setNodeData(zkClient, zonePath, agentConfig.toJson());
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentService.reportOnline(zone.getName(), buildKeys(zone.getName(), agents));
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zone, z -> new ZoneEventWatcher(z, zonePath));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    @Override
    public List<Zone> getZones() {
        return Lists.newArrayList(zoneEventWatchers.keySet());
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedRate = 120 * 1000)
    public void keepIdleAgent() {
        if (!AppConfig.ENABLE_KEEP_IDLE_AGENT_TASK) {
            System.out.println("ZoneService.keepIdleAgent: Task not enabled");
            return;
        }

        // get num of idle agent
        for (Zone zone : getZones()) {
            int numOfIdle = agentService.findAvailable(zone.getName()).size();
            System.out.println(String.format("Num of idle agent in zone %s = %s", zone.getName(), numOfIdle));

            // TODO: find related instance manager
            // find instance manager by zone
            String beanName = String.format("%sInstanceManager", zone.getCloudProvider());
            InstanceManager instanceManager = (InstanceManager) springContextUtil.getBean(beanName);
            if (instanceManager != null) {
                if (numOfIdle <= MIN_NUM_OF_IDLE_AGENT) {
                    instanceManager.batchStartInstance(MIN_NUM_OF_IDLE_AGENT * 2);
                }
            }
        }
    }

    private Collection<AgentPath> buildKeys(String zone, Collection<String> agents) {
        ArrayList<AgentPath> keys = new ArrayList<>(agents.size());
        for (String agentName : agents) {
            keys.add(new AgentPath(zone, agentName));
        }
        return keys;
    }

    /**
     * To handle zk events on zone level
     */
    private class ZoneEventWatcher implements Watcher {

        private final Zone zone;
        private final String zonePath;

        ZoneEventWatcher(Zone zone, String zonePath) {
            this.zone = zone;
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            zkHelper.recordEvent(zonePath, event);

            // continue to watch zone path
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                taskExecutor.execute(() -> {
                    List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentService.reportOnline(zone.getName(), buildKeys(zone.getName(), agents));
                });
            }
        }
    }
}
