package com.flow.platform.cc.service;

import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private String zkRootPath;

    @Value("${zk.node.zone}")
    private String zkZone;

    private final ExecutorService executorService;
    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    private ZooKeeper zk;
    private CountDownLatch initLatch = new CountDownLatch(1);
    private CountDownLatch zoneLatch;

    @Autowired
    public ZkServiceImpl(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        zk = new ZooKeeper(zkHost, zkTimeout, this);

        if (!initLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Cannot connect to zookeeper server within 10 seconds");
        }

        // init root node and watch children event
        ZkNodeHelper.createNode(zk, zkRootPath, "");
        ZkNodeHelper.watchChildren(zk, zkRootPath, this, 5);

        // init zone nodes
        String[] zones = zkZone.split(";");
        zoneLatch = new CountDownLatch(zones.length);
        for (String zone : zones) {
            createZone(zone);
        }

        if (!zoneLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Cannot init zone nodes within 10 seconds");
        }
    }

    public String createZone(String zoneName) {
        String zonePath = String.format("%s/%s", zkRootPath, zoneName);
        ZkNodeHelper.createNode(zk, zonePath, "");

        // get zk zone event watcher
        ZoneEventWatcher watcher = zoneEventWatchers.get(zoneName);
        if (watcher == null) {
            watcher = new ZoneEventWatcher(zonePath);
            zoneEventWatchers.put(zoneName, watcher);
        }

        ZkNodeHelper.watchNode(zk, zonePath, watcher, 5);
        return zonePath;
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

        if (ZkEventHelper.isChildrenChangedOnPath(event, zkRootPath)) {
            ZkNodeHelper.watchChildren(zk, zkRootPath, this, 5);
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
            try {
                System.out.println(event);
            } finally {
                ZkNodeHelper.watchNode(zk, zonePath, this, 5);
            }
        }
    }
}
