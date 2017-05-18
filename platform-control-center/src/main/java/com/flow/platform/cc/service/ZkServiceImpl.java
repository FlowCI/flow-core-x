package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkNodeBuilder;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.common.collect.Sets;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service(value = "zkService")
public class ZkServiceImpl implements ZkService {

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootName;

    @Value("${zk.node.zone}")
    private String zkZone;

    private final ExecutorService executorService;
    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    // zone path -> agent sets
    private final Map<String, Set<String>> onlineAgents = new ConcurrentHashMap<>(10);

    private ZooKeeper zk;
    private CountDownLatch initLatch = new CountDownLatch(1);
    private CountDownLatch zoneLatch;

    @Autowired
    public ZkServiceImpl(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Connect to zookeeper server and init root and zone nodes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        zk = new ZooKeeper(zkHost, zkTimeout, this);

        if (!initLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Cannot connect to zookeeper server within 10 seconds");
        }

        // init root node and watch children event
        String rootPath = ZkNodeBuilder.create(zkRootName).path();
        ZkNodeHelper.createNode(zk, rootPath, "");
        ZkNodeHelper.watchChildren(zk, rootPath, this, 5);

        // init zone nodes
        String[] zones = zkZone.split(";");
        zoneLatch = new CountDownLatch(zones.length);
        for (String zone : zones) {
            String zonePath = createZone(zone);
            onlineAgents.put(zonePath, Sets.<String>newConcurrentHashSet());
        }

        if (!zoneLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Cannot init zone nodes within 10 seconds");
        }
    }

    @Override
    public Set<String> onlineAgent(String zoneName) {
        String zonePath = ZkNodeBuilder.create(zkRootName).zone(zoneName).path();
        return onlineAgents.get(zonePath);
    }

    @Override
    public String createZone(String zoneName) {
        String zonePath = ZkNodeBuilder.create(zkRootName).zone(zoneName).path();
        ZkNodeHelper.createNode(zk, zonePath, "");

        // get zk zone event watcher
        ZoneEventWatcher watcher = zoneEventWatchers.get(zoneName);
        if (watcher == null) {
            watcher = new ZoneEventWatcher(zonePath);
            zoneEventWatchers.put(zoneName, watcher);
        }

        // watch children node for agents
        ZkNodeHelper.watchChildren(zk, zonePath, watcher, 5);
        return zonePath;
    }

    @Override
    public void sendCommand(String zoneName, String agentName, ZkCmd cmd) {
        Set<String> agents = onlineAgent(zoneName);
        ZkNodeBuilder pathBuilder = ZkNodeBuilder.create(zkRootName).zone(zoneName).agent(agentName);
        String agentNodePath = pathBuilder.path();

        if (!agents.contains(agentName) || ZkNodeHelper.exist(zk, agentNodePath) == null) {
            throw new AgentErr.NotFoundException(agentName);
        }

        if (agents.contains(String.format("%s-busy", agentName)) || ZkNodeHelper.exist(zk, pathBuilder.busy()) != null) {
            throw new AgentErr.BusyException(agentName);
        }

        ZkNodeHelper.setNodeData(zk, agentNodePath, cmd.toJson());
    }

    /**
     * To handle zk events on root level
     *
     * @param event
     */
    public void process(WatchedEvent event) {
        if (ZkEventHelper.isConnectToServer(event)) {
            initLatch.countDown();
        }

        String rootPath = ZkNodeBuilder.create(zkRootName).path();
        if (ZkEventHelper.isChildrenChangedOnPath(event, rootPath)) {
            ZkNodeHelper.watchChildren(zk, rootPath, this, 5);
            zoneLatch.countDown();
        }
    }

    /**
     * To handle zk event on zone level
     */
    private class ZoneEventWatcher implements Watcher {

        private String zonePath;

        ZoneEventWatcher(String zonePath) {
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            System.out.println(event);
            // continue to watch zone path
            ZkNodeHelper.watchChildren(zk, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: to update online agent db
                        List<String> childrenNodes = ZkNodeHelper.getChildrenNodes(zk, zonePath);

                        Set<String> agentsInZone = onlineAgents.get(zonePath);
                        agentsInZone.clear();
                        agentsInZone.addAll(childrenNodes);
                    }
                });
            }
        }
    }
}
