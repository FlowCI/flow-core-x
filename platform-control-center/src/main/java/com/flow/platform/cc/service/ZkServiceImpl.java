package com.flow.platform.cc.service;

import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentConfig agentConfig;

    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    private final ExecutorService executorService;

    private ZooKeeper zkClient = null;

    private CountDownLatch zkConnectLatch = null;

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
        zkClient = reconnect();

        // init root node and watch children event
        String rootPath = ZkPathBuilder.create(zkRootName).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes
        for (String zone : definedZones()) {
            createZone(zone);
        }
    }

    @Override
    public ZooKeeper zkClient() {
        if (zkClient == null) {
            try {
                zkClient = reconnect();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return zkClient;
    }

    @Override
    public ZkPathBuilder buildZkPath(String zone, String name) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        if (zone != null) {
            pathBuilder.append(zone);
            if (name != null) {
                pathBuilder.append(name);
            }
        }
        return pathBuilder;
    }

    @Override
    public String[] definedZones() {
        return zkZone.split(";");
    }

    @Override
    public String createZone(String zoneName) {
        String zonePath = ZkPathBuilder.create(zkRootName).append(zoneName).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, agentConfig.toJson());
        } else{
            ZkNodeHelper.setNodeData(zkClient, zonePath, agentConfig.toJson());
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentService.reportOnline(buildKeys(zoneName, agents));
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zonePath, p -> new ZoneEventWatcher(zoneName, p));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    /**
     * Reconnect to zk
     */
    private ZooKeeper reconnect() throws IOException, InterruptedException {
        zkConnectLatch = new CountDownLatch(1);
        ZooKeeper zk = new ZooKeeper(zkHost, zkTimeout, new ZkRootHandler());
        if (!zkConnectLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException(String.format("Cannot connect to zookeeper server '%s' within 10 seconds", zkHost));
        }
        return zk;
    }

    private Collection<AgentPath> buildKeys(String zone, Collection<String> agents) {
        ArrayList<AgentPath> keys = new ArrayList<>(agents.size());
        for (String agentName : agents) {
            keys.add(new AgentPath(zone, agentName));
        }
        return keys;
    }


    /**
     * To handle zk root events
     */
    private class ZkRootHandler implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            if (ZkEventHelper.isConnectToServer(event)) {
                zkConnectLatch.countDown();
            }

            if (ZkEventHelper.isSessionExpired(event)) {
                try {
                    zkClient = reconnect();
                } catch (Throwable e) {
                    // TODO: should handle zk connection exception
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
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
            System.out.println(event);
            // continue to watch zone path
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                executorService.execute(() -> {
                    List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentService.reportOnline(buildKeys(zoneName, agents));
                });
            }
        }
    }
}
