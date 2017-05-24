package com.flow.platform.cc.service;

import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service(value = "zkService")
public class ZkServiceImpl implements ZkService {

    private final Map<String, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();
    private final AgentDao agentDao;
    private final ExecutorService executorService;

    private final String zkRootName;
    private final String[] zkDefinedZones;
    private final ZooKeeper zkClient;

    @Autowired
    public ZkServiceImpl(ExecutorService executorService,
                         AgentDao agentDao,
                         String zkRootName,
                         String[] zkDefinedZones,
                         ZooKeeper zkClient) {
        this.executorService = executorService;
        this.agentDao = agentDao;
        this.zkRootName = zkRootName;
        this.zkClient = zkClient;
        this.zkDefinedZones = zkDefinedZones;
    }

    /**
     * Connect to zookeeper server and init root and zone nodes
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @PostConstruct
    public void init() throws IOException, InterruptedException {
        // init root node and watch children event
        String rootPath = ZkPathBuilder.create(zkRootName).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes
        for (String zone : zkDefinedZones) {
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
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, "");
        } else{
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentDao.reload(zoneName, agents);
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zonePath, p -> new ZoneEventWatcher(zoneName, p));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
        return zonePath;
    }

    @Override
    public Cmd sendCommand(CmdBase cmd) {
        Set<String> agents = onlineAgent(cmd.getZone());
        ZkPathBuilder pathBuilder = ZkPathBuilder.create(zkRootName).append(cmd.getZone()).append(cmd.getAgent());
        String agentNodePath = pathBuilder.path();

        try {
            if (!agents.contains(cmd.getAgent()) || ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(cmd.getAgent());
            }

            // check is busy
            if (agents.contains(ZkPathBuilder.busyNodeName(cmd.getAgent()))
                    || ZkNodeHelper.exist(zkClient, pathBuilder.busy()) != null) {
                // check command type
                if (cmd.getType() == Cmd.Type.RUN_SHELL) {
                    throw new AgentErr.BusyException(cmd.getAgent());
                }
            }

            // set cmd info
            String cmdId = UUID.randomUUID().toString();
            Cmd cmdInfo = new Cmd(cmd);
            cmdInfo.setId(cmdId);


            // send data
            ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmdInfo.toJson());
            return cmdInfo;

        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
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
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                executorService.execute(() -> {
                    List<String> childrenNodes = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentDao.reload(zoneName, childrenNodes);
                });
            }
        }
    }
}
