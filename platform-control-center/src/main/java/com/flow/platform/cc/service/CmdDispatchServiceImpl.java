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

import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.base.Strings;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    protected ZKClient zkClient;

    private final Map<CmdType, CmdHandler> handler = new HashMap<>(CmdType.values().length);

    @PostConstruct
    public void init() {
        CreateSessionCmdHandler createSessionHandler = new CreateSessionCmdHandler();
        handler.put(createSessionHandler.handleType(), createSessionHandler);

        DeleteSessionCmdHandler deleteSessionHandler = new DeleteSessionCmdHandler();
        handler.put(deleteSessionHandler.handleType(), deleteSessionHandler);

        RunShellCmdHandler runShellHandler = new RunShellCmdHandler();
        handler.put(runShellHandler.handleType(), runShellHandler);
    }

    @Override
    public Cmd dispatch(String cmdId, boolean reset) {
        Cmd cmd = cmdService.find(cmdId);
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

        handler.get(cmd.getType()).exec(target, cmd);

        // finally update cmd and send to agent
        cmdService.save(cmd);

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

    /**
     * Interface to handle different cmd type exec logic
     */
    private interface CmdHandler {

        CmdType handleType();

        void exec(Agent target, Cmd cmd);
    }

    private class CreateSessionCmdHandler implements CmdHandler {

        private final Logger logger = new Logger(CreateSessionCmdHandler.class);

        @Override
        public CmdType handleType() {
            return CmdType.CREATE_SESSION;
        }

        @Override
        public void exec(Agent target, Cmd cmd) {
            if (!target.isAvailable()) {
                throw new AgentErr.NotAvailableException(target.getName());
            }

            String existSessionId = cmd.getSessionId();

            // set session id to agent if session id does not from cmd
            if (Strings.isNullOrEmpty(existSessionId)) {
                existSessionId = UUID.randomUUID().toString();
            }

            target.setSessionId(existSessionId);
            target.setSessionDate(ZonedDateTime.now());
            target.setStatus(AgentStatus.BUSY);
            agentService.save(target);
            logger.debug("Agent session been created: %s %s", target.getPath(), target.getSessionId());
        }
    }

    private class DeleteSessionCmdHandler implements CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.DELETE_SESSION;
        }

        @Override
        public void exec(Agent target, Cmd cmd) {
            if (hasRunningCmd(target.getSessionId())) {
                // send kill cmd
                Cmd killCmd = cmdService.create(new CmdInfo(target.getPath(), CmdType.KILL, null));
                sendCmdToAgent(target, killCmd);
            }

            // release session from target
            target.setStatus(AgentStatus.IDLE);
            target.setSessionId(null);
            agentService.save(target);
        }

        private boolean hasRunningCmd(String sessionId) {
            List<Cmd> cmdsInSession = cmdService.listBySession(sessionId);
            for (Cmd cmd : cmdsInSession) {
                if (cmd.isCurrent() && cmd.getType() == CmdType.RUN_SHELL) {
                    return true;
                }
            }
            return false;
        }
    }

    private class RunShellCmdHandler implements CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.RUN_SHELL;
        }

        @Override
        public void exec(Agent target, Cmd cmd) {
            // FIXME: agent maybe really busy
            if (cmd.hasSession()) {
                sendCmdToAgent(target, cmd);
                return;
            }

            // add reject status since busy
            if (!target.isAvailable()) {
                throw new AgentErr.NotAvailableException(target.getName());
            }

            target.setStatus(AgentStatus.BUSY);
            agentService.save(target);
        }
    }
}
