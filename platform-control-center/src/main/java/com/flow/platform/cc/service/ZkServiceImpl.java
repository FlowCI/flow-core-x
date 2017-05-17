package com.flow.platform.cc.service;

import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service
public class ZkServiceImpl implements ZkService {

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootPath;

    @Value("${zk.node.zone}")
    private String zkZone;

    private ZooKeeper zk;
    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        zk = new ZooKeeper(zkHost, zkTimeout, this);

        // init root node
        ZkNodeHelper.createNode(zk, zkRootPath, "");

        // init zone nodes
        String[] zones = zkZone.split(";");
        for (String zone : zones) {
            createZone(zone);
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

        ZkNodeHelper.monitoringNode(zk, zonePath, watcher, 5);
        return zonePath;
    }

    /**
     * To handle zk events
     *
     * @param event
     */
    public void process(WatchedEvent event) {

    }

    private class ZoneEventWatcher implements Watcher {

        private String zonePath;

        ZoneEventWatcher(String zonePath) {
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            try {
                System.out.println(event);
            } finally {
                ZkNodeHelper.monitoringNode(zk, zonePath, this, 5);
            }
        }
    }
}
