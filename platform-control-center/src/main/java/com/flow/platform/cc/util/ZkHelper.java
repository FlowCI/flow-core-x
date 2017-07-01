package com.flow.platform.cc.util;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectUtil;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.base.Strings;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    /**
     * Zone dynamic property naming rule
     * - First %s is zone name
     * - Seconds %s is property name xx_yy_zz from Zone.getXxYyZz
     */
    private final static String ZONE_PROPERTY_NAMING = "zone.%s.%s";

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.root}")
    private String zkRootName;

    @Value("${zk.node.zone}")
    private String zkZone;

    @Autowired
    private Environment env;

    private CountDownLatch zkConnectLatch = null;

    private ZooKeeper zkClient = null;

    private ZkStatus zkStatus = ZkStatus.UNKNOWN;

    private final List<Zone> defaultZones = new ArrayList<>(5);

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
        initZones();
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
    public List<Zone> getDefaultZones() {
        return defaultZones;
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
        return new ZkInfo(zkHost, zkTimeout, zkRootName, getDefaultZones(), zkStatus);
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

    @PreDestroy
    public void destroy() {
        try {
            zkClient.close();
            LOGGER.trace("Zookeeper client closed");
        } catch (InterruptedException e) {
            LOGGER.error("Error on close zookeeper client", e);
        }
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
     * Load default zones from app.properties
     */
    private void initZones() {
        String[] zoneAndProviderList = zkZone.split(";");

        for (String zoneName : zoneAndProviderList) {
            Zone zone = new Zone();
            zone.setName(zoneName);
            fillZoneProperties(zone);
            defaultZones.add(zone);
        }
    }

    /**
     * Dynamic fill zone properties from app.properties to Zone instance
     *
     * @param emptyZone
     */
    private void fillZoneProperties(Zone emptyZone) {
        Field[] fields = ObjectUtil.getFields(Zone.class);
        for (Field field : fields) {
            String flatted = ObjectUtil.convertFieldNameToFlat(field.getName());
            String valueFromConfig = env.getProperty(String.format(ZONE_PROPERTY_NAMING, emptyZone.getName(), flatted));

            // assign value to bean
            if (!Strings.isNullOrEmpty(valueFromConfig)) {
                ObjectUtil.assignValueToField(field, emptyZone, valueFromConfig);
            }
        }
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
