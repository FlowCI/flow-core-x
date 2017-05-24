package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentKey;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl implements AgentService {

    private final Map<AgentKey, Agent> mockAgentStore = Maps.newConcurrentMap();

    @Autowired
    private ZkService zkService;

    @Override
    public void reportOnline(Collection<AgentKey> keys) {
        // find offline agent
        HashSet<AgentKey> offlines = new HashSet<>(mockAgentStore.keySet());
        offlines.removeAll(keys);

        // remote from online list and update status
        for (AgentKey key : offlines) {
            Agent offlineAgent = mockAgentStore.get(key);
            offlineAgent.setStatus(Agent.Status.OFFLINE);
            mockAgentStore.remove(key);
        }

        // fine newly online agent
        HashSet<AgentKey> onlines = new HashSet<>(keys);
        onlines.removeAll(mockAgentStore.keySet());

        // report online
        for (AgentKey key : onlines) {
            reportOnline(key);
        }
    }

    @Override
    public Agent find(AgentKey key) {
        return mockAgentStore.get(key);
    }

    @Override
    public Collection<Agent> onlineList(String zone) {
        Collection<Agent> zoneAgents = new ArrayList<>(mockAgentStore.size());
        for (Agent agent : mockAgentStore.values()) {
            if (agent.getZone().equals(zone)) {
                zoneAgents.add(agent);
            }
        }
        return zoneAgents;
    }

    @Override
    public Cmd sendCommand(CmdBase cmd) {
        Agent target = find(new AgentKey(cmd.getZone(), cmd.getAgent()));

        ZkPathBuilder pathBuilder = zkService.getPathBuilder(cmd.getZone(), cmd.getAgent());
        String agentNodePath = pathBuilder.path();

        try {
            // check agent is exist
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

    private void reportOnline(AgentKey key) {
        Agent exist = find(key);
        if (exist == null) {
            Agent agent = new Agent(key);
            agent.setStatus(Agent.Status.IDLE);
            mockAgentStore.put(key, agent);
        }
    }
}
