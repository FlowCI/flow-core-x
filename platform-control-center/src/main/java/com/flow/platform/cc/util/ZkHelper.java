package com.flow.platform.cc.util;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper raw data and root event handler
 * <p>
 * Created by gy@fir.im on 28/05/2017.
 * Copyright fir.im
 */
@Component(value = "zkHelper")
public class ZkHelper {

    public static class ZkInfo {
        private String host;
        private Integer timeout;
        private String root;
        private List<Zone> zones;
        private ZkStatus status;

        public ZkInfo(String host, Integer timeout, String root, List<Zone> zones, ZkStatus status) {
            this.host = host;
            this.timeout = timeout;
            this.root = root;
            this.zones = zones;
            this.status = status;
        }

        public String getHost() {
            return host;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public String getRoot() {
            return root;
        }

        public List<Zone> getZones() {
            return zones;
        }

        public ZkStatus getStatus() {
            return status;
        }
    }

    public enum ZkStatus {
        UNKNOWN, OK, WARNING
    }

    private final static Logger LOGGER = new Logger(ZkHelper.class);

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

    private ZkStatus zkStatus = ZkStatus.UNKNOWN;

    // path, event list history
    private final Map<String, List<String>> eventHistory = new ConcurrentHashMap<>();

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
     * Get zk root node name
     *
     * @return
     */
    public String getRoot() {
        return zkRootName;
    }

    /**
     * Get predefined zones list
     *
     * @return
     */
    public List<Zone> getZones() {
        String[] zoneAndProviderList = zkZone.split(";");
        List<Zone> zoneList = new ArrayList<>(zoneAndProviderList.length);
        for (String zoneAndProvider : zoneAndProviderList) {
            String[] zoneAndProviderData = zoneAndProvider.split("=");
            zoneList.add(new Zone(zoneAndProviderData[0], zoneAndProviderData[1]));
        }
        return zoneList;
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

    public ZkInfo getInfo() {
        return new ZkInfo(zkHost, zkTimeout, zkRootName, getZones() , zkStatus);
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
        pathBuilder.append(cmd.getZone()).append(cmd.getAgentName());
        return pathBuilder.path();
    }

    /**
     * Record zk event to history
     *
     * @param path
     * @param event
     */
    public void recordEvent(String path, WatchedEvent event) {
        List<String> historyList = eventHistory.computeIfAbsent(path, k -> new LinkedList<>());
        String history = String.format("[%s] %s", AppConfig.APP_DATE_FORMAT.format(ZonedDateTime.now()), event.toString());
        historyList.add(history);

        if (ZkEventHelper.isConnectToServer(event)) {
            zkStatus = ZkStatus.OK;
        }

        if (ZkEventHelper.isDisconnected(event) || ZkEventHelper.isSessionExpired(event)) {
            zkStatus = ZkStatus.WARNING;
        }
    }

    public Map<String, List<String>> getZkHistory() {
        return eventHistory;
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
            recordEvent(zkRootName, event);
            LOGGER.traceMarker("ZookeeperRootEventHandler", "Zookeeper event received %s", event.toString());

            if (ZkEventHelper.isConnectToServer(event)) {
                zkConnectLatch.countDown();
            }

            if (ZkEventHelper.isSessionExpired(event)) {
                try {
                    zkClient = reconnect();
                } catch (Throwable e) {
                    LOGGER.errorMarker("ZookeeperRootEventHandler", "Error on reconnect to zookeeper when session expired", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }
}
