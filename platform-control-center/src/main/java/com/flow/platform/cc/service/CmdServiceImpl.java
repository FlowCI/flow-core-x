package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@Service(value = "cmdService")
public class CmdServiceImpl implements CmdService {

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZkService zkService;

    @Override
    public Cmd send(CmdBase cmd) {
        Agent target = agentService.find(new AgentPath(cmd.getZone(), cmd.getAgent()));

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
}
