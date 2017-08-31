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

import static com.flow.platform.domain.CmdType.*;

import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.base.Strings;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class CmdDispatchServiceImpl implements CmdDispatchService {

    private final static Logger LOGGER = new Logger(CmdDispatchService.class);

    @Autowired
    private CmdDao cmdDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    protected ZKClient zkClient;

    @Override
    public Cmd dispatch(String cmdId, boolean reset) {
        Cmd cmd = cmdDao.get(cmdId);
        if (cmd == null) {
            throw new IllegalParameterException(String.format("Cmd '%s' does not exist", cmdId));
        }

        // reset cmd status to pending
        if (reset) {
            cmd.setStatus(CmdStatus.PENDING);
        }

        // get target agent
        Agent target = selectAgent(cmd);
        cmd.setAgentPath(target.getPath());
        LOGGER.traceMarker("dispatch", "Agent been selected %s with status %s", target.getPath(), target.getStatus());

        // finally update cmd
        cmdDao.update(cmd);

        if (cmd.isAgentCmd()) {
            sendCmdToAgent(target, cmd);
        }

        return cmd;
    }

    /**
     * Select agent by AgentPath or session id
     * - auto select agent if only defined zone name
     *
     * @return Agent or null
     * @throws AgentErr.NotAvailableException no idle agent in zone
     * @throws AgentErr.AgentMustBeSpecified name must for operation cmd type
     * @throws AgentErr.NotFoundException target agent not found
     */
    private Agent selectAgent(Cmd cmd) {
        // check session id as top priority
        if (cmd.hasSession()) {
            Agent target = agentService.find(cmd.getSessionId());
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getSessionId());
            }
            return target;
        }

        AgentPath cmdTargetPath = cmd.getAgentPath();

        // auto select agent from zone if agent name not defined
        if (Strings.isNullOrEmpty(cmdTargetPath.getName())) {
            List<Agent> availableList = agentService.findAvailable(cmdTargetPath.getZone());
            if (availableList.size() > 0) {
                Agent target = availableList.get(0);
                cmd.setAgentPath(target.getPath()); // reset cmd path
                return target;
            }

            throw new AgentErr.NotAvailableException(cmdTargetPath.getZone());
        }

        // find agent by path
        Agent target = agentService.find(cmdTargetPath);
        if (target == null) {
            throw new AgentErr.NotFoundException(cmd.getAgentName());
        }

        return target;
    }



    private boolean isValidAgentPath(CmdBase cmd, AgentPath agentPath) {
        if (cmd.getType() == CREATE_SESSION || cmd.getType() == DELETE_SESSION) {
            return true;
        }

        return agentPath.getName() == null && cmd.getType() != RUN_SHELL;
    }

    /**
     * Send cmd to agent via zookeeper
     */
    private void sendCmdToAgent(Agent target, Cmd cmd) {
        String agentNodePath = ZKHelper.buildPath(target.getPath());
        if (!zkClient.exist(agentNodePath)) {
            throw new AgentErr.NotFoundException("Node path in zookeeper not found " + target.getPath());
        }

        zkClient.setData(agentNodePath, cmd.toBytes());
    }
}
