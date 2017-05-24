package com.flow.platform.cc.service;

import com.flow.platform.domain.AgentKey;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service(value = "zkService")
public class ZkServiceImpl implements ZkService {

    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();
    private final ExecutorService executorService;

    private final String zkRootName;
    private final String[] zkDefinedZones;
    private final ZooKeeper zkClient;

    private final AgentService agentService;

    @Autowired
    public ZkServiceImpl(ExecutorService executorService,
                         String zkRootName,
                         String[] zkDefinedZones,
                         ZooKeeper zkClient, AgentService agentService) {
        this.executorService = executorService;
        this.zkRootName = zkRootName;
        this.zkClient = zkClient;
        this.zkDefinedZones = zkDefinedZones;
        this.agentService = agentService;
    }

    /**
     * Connect to zookeeper server and init root and zone nodes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        // init root node and watch children event
        String rootPath = ZkPathBuilder.create(zkRootName).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes
        for (String zone : zkDefinedZones) {
            createZone(zone);
        }
    }

    @Override
    public String createZone(String zoneName) {
        String zonePath = ZkPathBuilder.create(zkRootName).append(zoneName).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, "");
        } else{
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentService.register(buildKeys(zoneName, agents));
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zonePath, p -> new ZoneEventWatcher(zoneName, p));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    private Collection<AgentKey> buildKeys(String zone, Collection<String> agents) {
        ArrayList<AgentKey> keys = new ArrayList<>(agents.size());
        for (String agentName : agents) {
            keys.add(new AgentKey(zone, agentName));
        }
        return keys;
    }

    /**
     * To handle zk event on zone level
     */
    private class ZoneEventWatcher implements Watcher {

        private String zoneName;
        private String zonePath;

        ZoneEventWatcher(String zoneName, String zonePath) {
            this.zoneName = zoneName;
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            System.out.println(event);
            // continue to watch zone path
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                executorService.execute(() -> {
                    List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentService.register(buildKeys(zoneName, agents));
                });
            }
        }
    }
}
