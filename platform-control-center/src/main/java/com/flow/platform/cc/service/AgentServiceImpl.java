package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl implements AgentService {

    private final Map<AgentPath, Agent> agentOnlineList = Maps.newConcurrentMap();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZkService zkService;

    @Override
    public void reportOnline(Collection<AgentPath> keys) {
        onlineListUpdateLock.lock();
        try {
            // find offline agent
            HashSet<AgentPath> offlines = new HashSet<>(agentOnlineList.keySet());
            offlines.removeAll(keys);

            // remote from online list and update status
            for (AgentPath key : offlines) {
                Agent offlineAgent = agentOnlineList.get(key);
                offlineAgent.setStatus(Agent.Status.OFFLINE);
                agentOnlineList.remove(key);
            }

            // fine newly online agent
            HashSet<AgentPath> onlines = new HashSet<>(keys);
            onlines.removeAll(agentOnlineList.keySet());

            // report online
            for (AgentPath key : onlines) {
                reportOnline(key);
            }
        } finally {
            onlineListUpdateLock.unlock();
        }
    }

    @Override
    public Agent find(AgentPath key) {
        return agentOnlineList.get(key);
    }

    @Override
    public Collection<Agent> onlineList(String zone) {
        Collection<Agent> zoneAgents = new ArrayList<>(agentOnlineList.size());
        for (Agent agent : agentOnlineList.values()) {
            if (agent.getZone().equals(zone)) {
                zoneAgents.add(agent);
            }
        }
        return zoneAgents;
    }

    @Override
    public Cmd sendCommand(CmdBase cmd) {
        Agent target = find(new AgentPath(cmd.getZone(), cmd.getAgent()));

        ZkPathBuilder pathBuilder = zkService.buildZkPath(cmd.getZone(), cmd.getAgent());
        String agentNodePath = pathBuilder.path();

        try {
            // check agent is online
            if (target == null || ZkNodeHelper.exist(zkService.zkClient(), agentNodePath) == null) {
                throw new AgentErr.NotFoundException(cmd.getAgent());
            }

            // check agent status is idle
            if (target.getStatus() != Agent.Status.IDLE) {
                throw new AgentErr.NotAvailableException(cmd.getAgent());
            }

            // set cmd info
            String cmdId = UUID.randomUUID().toString();
            Cmd cmdInfo = new Cmd(cmd);
            cmdInfo.setId(cmdId);

            // send data
            ZkNodeHelper.setNodeData(zkService.zkClient(), agentNodePath, cmdInfo.toJson());

            // update agent status
            target.setStatus(Agent.Status.BUSY);
            return cmdInfo;

        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        }
    }

    @Override
    public void reportStatus(AgentPath path, Agent.Status status) {
        Agent exist = find(path);
        if (exist == null) {
            throw new AgentErr.NotFoundException(path.getName());
        }
        exist.setStatus(status);
    }

    /**
     * Find from online list and create
     *
     * @param key
     */
    private void reportOnline(AgentPath key) {
        Agent exist = find(key);
        if (exist == null) {
            Agent agent = new Agent(key);
            agent.setStatus(Agent.Status.IDLE);
            agentOnlineList.put(key, agent);
        }
    }
}
