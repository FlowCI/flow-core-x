package com.flow.platform.cc.config;

import com.flow.platform.util.zk.ZkEventHelper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */

@Configurable
public class ZkConfig {

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootName;

    @Value("${zk.node.zone}")
    private String zkZone;

    private CountDownLatch zkConnectLatch = new CountDownLatch(1);

    @Bean
    private ZooKeeper zkClient() throws IOException, InterruptedException {
        ZooKeeper zk = new ZooKeeper(zkHost, zkTimeout, new ZkHandler());
        if (!zkConnectLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException(String.format("Cannot connect to zookeeper server '%s' within 10 seconds", zkHost));
        }
        return zk;
    }

    @Bean
    private String zkRootName() {
        return zkRootName;
    }

    @Bean
    private String[] zkDefinedZones() {
        return zkZone.split(";");
    }

    /**
     * To handle
     */
    private class ZkHandler implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            if (ZkEventHelper.isConnectToServer(event)) {
                zkConnectLatch.countDown();
            }
        }
    }
}
