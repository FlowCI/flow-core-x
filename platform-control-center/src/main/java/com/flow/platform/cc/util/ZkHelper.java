package com.flow.platform.cc.util;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 28/05/2017.
 * Copyright fir.im
 */
@Component(value = "zkHelper")
public class ZkHelper {

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootName;

    @Value("${zk.node.zone}")
    private String zkZone;

    private CountDownLatch zkConnectLatch = null;

    private ZooKeeper zkClient = null;

    /**
     * Connect to zookeeper server and init root and zone nodes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        zkClient = reconnect();
    }

    /**
     * Get predefined zones list
     *
     * @return
     */
    public String[] getZones() {
        return zkZone.split(";");
    }

    /**
     * Get ZooKeeper client
     *
     * @return
     */
    public ZooKeeper getClient() {
        if (zkClient == null) {
            try {
                zkClient = reconnect();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return zkClient;
    }

    /**
     * Get zk path builder for agent
     *
     * @param zone zone name (nullable)
     * @param name agent name (nullable)
     * @return zone path builder
     */
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

    /**
     * Get zookeeper path from AgentPath object
     *
     * @param agentPath
     * @return
     */
    public String getZkPath(AgentPath agentPath) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        pathBuilder.append(agentPath.getZone()).append(agentPath.getName());
        return pathBuilder.path();
    }

    /**
     * Get zookeeper path from CmdBase object
     *
     * @param cmd
     * @return
     */
    public String getZkPath(CmdBase cmd) {
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName);
        pathBuilder.append(cmd.getZone()).append(cmd.getAgent());
        return pathBuilder.path();
    }

    /**
     * Reconnect to zk
     */
    private ZooKeeper reconnect() throws IOException, InterruptedException {
        zkConnectLatch = new CountDownLatch(1);
        ZooKeeper zk = new ZooKeeper(zkHost, zkTimeout, new RootEventHandler());
        if (!zkConnectLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException(String.format("Cannot connect to zookeeper server '%s' within 10 seconds", zkHost));
        }
        return zk;
    }

    /**
     * To handle zk root events
     */
    private class RootEventHandler implements Watcher {

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
}
