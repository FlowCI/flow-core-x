package com.flow.platform.cc.service;

import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Receive and process zookeeper event
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service
public class ZkService implements Watcher {

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootPath;

    private ZooKeeper zk;

    @PostConstruct
    public void init() throws IOException {
        zk = new ZooKeeper(zkHost, zkTimeout, this);

        // init root node
        ZkNodeHelper.createNode(zk, zkRootPath, "");

        //TODO: init zone nodes
    }

    /**
     * To handle zk events
     * @param event
     */
    public void process(WatchedEvent event) {

    }
}
