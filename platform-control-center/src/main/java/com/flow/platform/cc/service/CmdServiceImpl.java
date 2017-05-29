package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.*;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Service(value = "cmdService")
public class CmdServiceImpl extends ZkServiceBase implements CmdService {

    @Autowired
    private AgentService agentService;

    private final Map<String, Cmd> mockCmdList = new ConcurrentHashMap<>();

    @Override
    public Cmd create(CmdBase cmd) {
        String cmdId = UUID.randomUUID().toString();
        Cmd cmdInfo = new Cmd(cmd);
        cmdInfo.setId(cmdId);
        mockCmdList.put(cmdId, cmdInfo);
        return cmdInfo;
    }

    @Override
    public Cmd find(String cmdId) {
        return mockCmdList.get(cmdId);
    }

    @Override
    public Cmd send(CmdBase cmd) {
        AgentPath agentPath = new AgentPath(cmd.getZone(), cmd.getAgent());
        Agent target = agentService.find(agentPath);
        String agentNodePath = zkHelper.getZkPath(agentPath);

        try {
            // check agent is online
            if (target == null || ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(cmd.getAgent());
            }

            // create cmd info
            Cmd cmdInfo = create(cmd);

            switch (cmd.getType()) {
                case RUN_SHELL:
                    if (target.getStatus() != Agent.Status.IDLE) {
                        throw new AgentErr.NotAvailableException(cmd.getAgent());
                    }

                    target.setStatus(Agent.Status.BUSY);
                    break;

                case KILL:
                    // DO NOT handle it, agent status from cmd update
                    break;

                case STOP:
                    target.setStatus(Agent.Status.OFFLINE);
                    break;

                case SHUTDOWN:
                    target.setStatus(Agent.Status.OFFLINE);
                    break;
            }

            ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmdInfo.toJson());
            return cmdInfo;

        } catch (ZkException.ZkNoNodeException e) {
            throw new AgentErr.NotFoundException(cmd.getAgent());
        }
    }

    @Override
    public void report(String cmdId, Cmd.Status status, CmdResult result) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        // update cmd status
        cmd.setStatus(status);
        cmd.setResult(result);
        cmd.setUpdatedDate(new Date());

        // update agent status
        updateAgentStatusWhenUpdateCmd(cmd);
    }

    /**
     * Update agent status when report cmd status and result
     * - busy or idle by Cmd.Type.RUN_SHELL while report cmd status
     *
     * @param cmd Cmd object
     */
    private void updateAgentStatusWhenUpdateCmd(Cmd cmd) {
        AgentPath agentPath = cmd.getAgentPath();
        boolean isAgentBusy = false;
        for (Cmd tmp : mockCmdList.values()) {
            if (tmp.getType() != Cmd.Type.RUN_SHELL) {
                continue;
            }

            if (!tmp.getAgentPath().equals(agentPath)) {
                continue;
            }

            if (tmp.isCurrent()) {
                isAgentBusy = true;
                break;
            }
        }

        Agent agent = agentService.find(agentPath);
        if (agent == null) {
            throw new IllegalStateException("Cannot find related agent for cmd");
        }
        agent.setStatus(isAgentBusy ? Agent.Status.BUSY : Agent.Status.IDLE);
    }
}
