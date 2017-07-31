/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.util;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectUtil;
import com.flow.platform.util.zk.ZkEventHelper;
import com.google.common.base.Strings;
import org.apache.curator.utils.ZKPaths;
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
 * @author gy@fir.im
 */
@Component(value = "zkHelper")
public class ZkHelper {

    public static class ZkInfo {

        private String host;
        private Integer timeout;
        private String root;
        private List<Zone> zones;
        private ZkStatus status;

        ZkInfo(String host, Integer timeout, String root, List<Zone> zones, ZkStatus status) {
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
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        zkClient = reconnect();
        loadZones();
    }

    /**
     * Get predefined zones list
     */
    public List<Zone> getDefaultZones() {
        return defaultZones;
    }

    /**
     * Get ZooKeeper client
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
     * @return zone path as string
     */
    public String buildZkPath(String zone, String name) {
        return ZKPaths.makePath(zkRootName, zone, name);
    }

    /**
     * Get zookeeper path from AgentPath object
     */
    public String getZkPath(AgentPath agentPath) {
        return buildZkPath(agentPath.getZone(), agentPath.getName());
    }

    /**
     * Record zk event to history
     */
    public void recordEvent(String path, WatchedEvent event) {
        List<String> historyList = eventHistory.computeIfAbsent(path, k -> new LinkedList<>());
        String history = String
            .format("[%s] %s", AppConfig.APP_DATE_FORMAT.format(ZonedDateTime.now()), event.toString());
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
            throw new RuntimeException(
                String.format("Cannot connect to zookeeper server '%s' within 10 seconds", zkHost));
        }
        return zk;
    }

    /**
     * Load default zones from app.properties
     */
    private void loadZones() {
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
                    LOGGER.errorMarker("ZookeeperRootEventHandler",
                        "Error on reconnect to zookeeper when session expired", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }
}
