package com.flow.platform.cc.service;

import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final AgentDao agentDao;
    private final ExecutorService executorService;
    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    private ZooKeeper zk;
    private CountDownLatch initLatch = new CountDownLatch(1);

    @Autowired
    public ZkServiceImpl(ExecutorService executorService, AgentDao agentDao) {
        this.executorService = executorService;
        this.agentDao = agentDao;
    }

    /**
     * Connect to zookeeper server and init root and zone nodes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        zk = new ZooKeeper(zkHost, zkTimeout, this);

        if (!initLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Cannot connect to zookeeper server within 10 seconds");
        }

        // init root node and watch children event
        String rootPath = ZkPathBuilder.create(zkRootName).path();
        ZkNodeHelper.createNode(zk, rootPath, "");
        ZkNodeHelper.watchChildren(zk, rootPath, this, 5);

        // init zone nodes
        String[] zones = zkZone.split(";");
        for (String zone : zones) {
            createZone(zone);
        }
    }

    @Override
    public Set<String> onlineAgent(String zoneName) {
        return agentDao.online(zoneName);
    }

    @Override
    public String createZone(String zoneName) {
        String zonePath = ZkPathBuilder.create(zkRootName).append(zoneName).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zk, zonePath) == null){
            ZkNodeHelper.createNode(zk, zonePath, "");
        } else{
            List<String> agents = ZkNodeHelper.getChildrenNodes(zk, zonePath);
            agentDao.reload(zoneName, agents);
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zonePath, p -> new ZoneEventWatcher(zoneName, p));

        ZkNodeHelper.watchChildren(zk, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    @Override
    public void sendCommand(String zoneName, String agentName, Cmd cmd) {
        Set<String> agents = onlineAgent(zoneName);
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName).append(zoneName).append(agentName);
        String agentNodePath = pathBuilder.path();

        try {
            if (!agents.contains(agentName) || ZkNodeHelper.exist(zk, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(agentName);
            }

            // check is busy
            if (agents.contains(ZkPathBuilder.busyNodeName(agentName)) || ZkNodeHelper.exist(zk, pathBuilder.busy()) != null) {
                // check command type
                if (cmd.getType() == Cmd.Type.RUN_SHELL) {
                    throw new AgentErr.BusyException(agentName);
                }
            }

            ZkNodeHelper.setNodeData(zk, agentNodePath, cmd.toJson());

        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(agentName);
        }
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
    }

    /**
     * To handle zk event on zone level
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
            ZkNodeHelper.watchChildren(zk, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                executorService.execute(() -> {
                    List<String> childrenNodes = ZkNodeHelper.getChildrenNodes(zk, zonePath);
                    agentDao.reload(zoneName, childrenNodes);
                });
            }
        }
    }
}
