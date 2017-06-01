package com.flow.platform.cc.service;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private Executor taskExecutor;

    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    @PostConstruct
    private void init() {
        // init root node and watch children event
        String rootPath = zkHelper.buildZkPath(null, null).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes
        for (String zone : zkHelper.getZones()) {
            createZone(zone);
        }
    }

    @Override
    public String createZone(String zoneName) {
        String zonePath = zkHelper.buildZkPath(zoneName, null).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, agentConfig.toJson());
        } else{
            ZkNodeHelper.setNodeData(zkClient, zonePath, agentConfig.toJson());
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentService.reportOnline(zoneName, buildKeys(zoneName, agents));
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zonePath, p -> new ZoneEventWatcher(zoneName, p));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    @Override
    public List<String> getZones() {
        return ZkNodeHelper.getChildrenNodes(zkClient, zkHelper.getRoot());
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

        private String zoneName;
        private String zonePath;

        ZoneEventWatcher(String zoneName, String zonePath) {
            this.zoneName = zoneName;
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            zkHelper.recordEvent(zonePath, event);

            // continue to watch zone path
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                taskExecutor.execute(() -> {
                    List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentService.reportOnline(zoneName, buildKeys(zoneName, agents));
                });
            }
        }
    }
}
