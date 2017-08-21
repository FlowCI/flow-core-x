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

package com.flow.platform.cc.service;

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.cloud.InstanceManager;
import com.flow.platform.core.context.ContextEvent;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Instance;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gy@fir.im
 */
@Service(value = "zoneService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class ZoneServiceImpl implements ZoneService, ContextEvent {

    private final static Logger LOGGER = new Logger(ZoneService.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentSettings agentSettings;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private SpringContext springContext;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private List<Zone> defaultZones;

    @Autowired
    protected ZKClient zkClient;

    private final Map<Zone, ZoneEventListener> zoneEventWatchers = new HashMap<>();

    @Override
    public void start() {
        // init root node
        String path = createRoot();
        LOGGER.trace("Root zookeeper node initialized: %s", path);

        // init zone nodes
        for (Zone zone : defaultZones) {
            path = createZone(zone);
            LOGGER.trace("Zone zookeeper node initialized: %s", path);
        }
    }

    @Override
    public void stop() {
        // ignore
    }

    @Override
    public String createRoot() {
        String rootPath = ZKHelper.buildPath(null, null);
        return zkClient.create(rootPath, null);
    }

    @Override
    public String createZone(Zone zone) {
        final String zonePath = ZKHelper.buildPath(zone.getName(), null);
        zone.setPath(zonePath);

        zkClient.create(zonePath, agentSettings.toBytes());

        List<String> agents = zkClient.getChildren(zonePath);

        if (!agents.isEmpty()) {
            agentService.reportOnline(zone.getName(), Sets.newHashSet(agents));
        }

        ZoneEventListener zoneEventWatcher = zoneEventWatchers.computeIfAbsent(zone, ZoneEventListener::new);
        zkClient.watchChildren(zonePath, zoneEventWatcher);
        return zonePath;
    }

    @Override
    public Zone getZone(String zoneName) {
        for (Zone zone : zoneEventWatchers.keySet()) {
            if (Objects.equals(zoneName, zone.getName())) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public List<Zone> getZones() {
        return Lists.newArrayList(zoneEventWatchers.keySet());
    }

    @Override
    public InstanceManager findInstanceManager(Zone zone) {
        if (!zone.isAvailable()) {
            return null;
        }
        String beanName = String.format("%sInstanceManager", zone.getCloudProvider());
        return (InstanceManager) springContext.getBean(beanName);
    }

    /**
     * Find num of idle agent and batch start instance
     *
     * @return boolean true = need start instance, false = has enough idle agent
     */
    @Override
    public boolean keepIdleAgentMinSize(final Zone zone, final InstanceManager instanceManager) {
        int numOfIdle = agentService.findAvailable(zone.getName()).size();
        LOGGER.traceMarker("keepIdleAgentMinSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle < zone.getMinPoolSize()) {
            instanceManager.batchStartInstance(zone);
            return true;
        }

        return false;
    }

    /**
     * Find num of idle agent and check max pool size,
     * send shutdown cmd to agent and delete instance
     */
    @Override
    public boolean keepIdleAgentMaxSize(final Zone zone, final InstanceManager instanceManager) {
        List<Agent> agentList = agentService.findAvailable(zone.getName());
        int numOfIdle = agentList.size();
        LOGGER.traceMarker("keepIdleAgentMaxSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle > zone.getMaxPoolSize()) {
            int numOfRemove = numOfIdle - zone.getMaxPoolSize();

            for (int i = 0; i < numOfRemove; i++) {
                Agent idleAgent = agentList.get(i);

                // send shutdown cmd
                CmdInfo cmdInfo = new CmdInfo(idleAgent.getPath(), CmdType.SHUTDOWN, "flow.ci");
                cmdService.send(cmdInfo);
                LOGGER.traceMarker("keepIdleAgentMaxSize", "Send SHUTDOWN to idle agent: %s", idleAgent);

                // add instance to cleanup list
                Instance instance = instanceManager.find(idleAgent.getPath());
                if (instance != null) {
                    instanceManager.addToCleanList(instance);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = KEEP_IDLE_AGENT_TASK_PERIOD)
    public void keepIdleAgentTask() {
        if (!taskConfig.isEnableKeepIdleAgentTask()) {
            return;
        }

        LOGGER.traceMarker("keepIdleAgentTask", "start");

        // get num of idle agent
        for (Zone zone : getZones()) {
            InstanceManager instanceManager = findInstanceManager(zone);
            if (instanceManager == null) {
                continue;
            }

            if (keepIdleAgentMinSize(zone, instanceManager)) {
                continue;
            }

            keepIdleAgentMaxSize(zone, instanceManager);
        }

        LOGGER.traceMarker("keepIdleAgentTask", "end");
    }

    private class ZoneEventListener implements PathChildrenCacheListener {

        private final Zone zone;

        ZoneEventListener(Zone zone) {
            this.zone = zone;
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            // TODO: should optimize by event type

            taskExecutor.execute(() -> {
                List<String> agents = zkClient.getChildren(zone.getPath());
                agentService.reportOnline(zone.getName(), Sets.newHashSet(agents));
            });
        }
    }
}
