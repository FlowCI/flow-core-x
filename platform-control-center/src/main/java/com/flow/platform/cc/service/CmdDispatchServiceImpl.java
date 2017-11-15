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
import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.event.AgentResourceEvent;
import com.flow.platform.cc.event.AgentResourceEvent.Category;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.exception.AgentErr.NotAvailableException;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.flow.platform.util.zk.ZkException;
import com.google.common.base.Strings;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yang
 */
@Service
@Transactional
public class CmdDispatchServiceImpl extends ApplicationEventService implements CmdDispatchService {

    private final static Logger LOGGER = new Logger(CmdDispatchService.class);

    @Autowired
    private TaskConfig taskConfig;

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

        KillCmdHandler killHandler = new KillCmdHandler();
        handler.put(killHandler.handleType(), killHandler);

        StopCmdHandler stopHandler = new StopCmdHandler();
        handler.put(stopHandler.handleType(), stopHandler);

        ShutdownCmdHandler shutdownHandler = new ShutdownCmdHandler();
        handler.put(shutdownHandler.handleType(), shutdownHandler);

        SystemInfoCmdHandler runOtherCmdHandler = new SystemInfoCmdHandler();
        handler.put(runOtherCmdHandler.handleType(), runOtherCmdHandler);
    }

    @Override
    @Transactional(noRollbackFor = {Throwable.class})
    public Cmd dispatch(String cmdId, boolean reset) {
        Cmd cmd = cmdService.find(cmdId);
        if (cmd == null) {
            throw new IllegalParameterException(String.format("Cmd '%s' does not exist", cmdId));
        }

        // reset cmd status to pending
        if (reset) {
            cmd.setStatus(CmdStatus.PENDING);
        }

        // do not run cmd if not in working status
        if (!cmd.isCurrent()) {
            return cmd;
        }

        try {
            handler.get(cmd.getType()).exec(cmd);
            cmd = cmdService.find(cmd.getId());
            return cmd;

        } catch (FlowException e) {
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            cmdService.updateStatus(statusItem, false);

            // broadcast AgentResourceEvent with zone name
            if (e instanceof NotAvailableException) {
                this.dispatchEvent(new AgentResourceEvent(this, cmd.getAgentPath().getZone(), Category.FULL));
            }

            throw e;

        } catch (ZkException e) {
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            cmdService.updateStatus(statusItem, false);
            throw new AgentErr.NotFoundException(cmd.getAgentName());

        } catch (Throwable e) {
            CmdResult result = new CmdResult();
            result.getExceptions().add(e);

            // update cmd status to exception
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.EXCEPTION, null, false, true);
            cmdService.updateStatus(statusItem, false);

            cleanCurrentCmd(cmd);
            throw e;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    @Scheduled(fixedDelay = 300 * 1000)
    public void checkTimeoutTask() {
        if (!taskConfig.isEnableCmdExecTimeoutTask()) {
            return;
        }
        LOGGER.traceMarker("checkTimeoutTask", "start");

        // find all running status cmd
        List<Cmd> workingCmdList = cmdService.listWorkingCmd(null);

        for (Cmd cmd : workingCmdList) {
            if (cmd.isCmdTimeout()) {
                Cmd killCmd = cmdService.create(new CmdInfo(cmd.getAgentPath(), CmdType.KILL, null));
                dispatch(killCmd.getId(), false);
                LOGGER.traceMarker("checkTimeoutTask", "Send KILL for timeout cmd %s", cmd);

                // update cmd status via queue
                CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.TIMEOUT_KILL, null, true, true);
                cmdService.updateStatus(statusItem, true);
            }
        }

        LOGGER.traceMarker("checkTimeoutTask", "end");
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

    private Cmd createDeleteSessionCmd(Agent target) {
        CmdInfo param = new CmdInfo(target.getPath(), CmdType.DELETE_SESSION, null);
        param.setSessionId(target.getSessionId());
        return cmdService.create(param);
    }

    /**
     * Kill agent current running cmd or delete current session
     */
    private void cleanCurrentCmd(Cmd current) {
        if (Strings.isNullOrEmpty(current.getSessionId())) {
            Cmd cmdToKill = cmdService.create(new CmdInfo(current.getAgentPath(), CmdType.KILL, null));
            dispatch(cmdToKill.getId(), false);
        } else {
            Agent agent = agentService.find(current.getAgentPath());
            Cmd cmdToDelSession = createDeleteSessionCmd(agent);
            dispatch(cmdToDelSession.getId(), false);
        }
    }

    /**
     * Auto select agent from zone
     *
     * @throws AgentErr.NotAvailableException if no available agent
     */
    private Agent selectAgentFromZone(String zone) {
        List<Agent> availableList = agentService.findAvailable(zone);

        if (availableList.size() > 0) {
            return availableList.get(0);
        }

        throw new AgentErr.NotAvailableException(zone);
    }

    /**
     * Interface to handle different cmd type exec logic
     */
    private abstract class CmdHandler {

        abstract CmdType handleType();

        abstract Agent select(Cmd cmd);

        abstract void doExec(Agent target, Cmd cmd);

        void exec(Cmd cmd) {
            Agent agent = select(cmd);

            if (agent != null) {
                cmd.setAgentPath(agent.getPath());
                cmdService.save(cmd);
            }

            doExec(agent, cmd);

            // update cmd status to SENT
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.SENT, null, false, true);
            cmdService.updateStatus(statusItem, false);
        }
    }

    private class CreateSessionCmdHandler extends CmdHandler {

        private final Logger logger = new Logger(CreateSessionCmdHandler.class);

        @Override
        public CmdType handleType() {
            return CmdType.CREATE_SESSION;
        }

        /**
         * - Auto select agent by zone
         * - Get agent from zone and name
         */
        @Override
        public Agent select(Cmd cmd) {
            AgentPath path = cmd.getAgentPath();

            if (!path.hasName()) {
                return selectAgentFromZone(path.getZone());
            }

            return agentService.find(path);
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getAgentPath().toString());
            }

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
            agentService.saveWithStatus(target, AgentStatus.BUSY);

            logger.debug("Agent session been created: %s %s", target.getPath(), target.getSessionId());
        }
    }

    private class DeleteSessionCmdHandler extends CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.DELETE_SESSION;
        }

        @Override
        Agent select(Cmd cmd) {
            return agentService.find(cmd.getSessionId());
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            // stop all cmd when agent not been assigned
            if (target == null) {
                for (Cmd cmdItem : cmdService.listBySession(cmd.getSessionId())) {
                    cmdItem.setStatus(CmdStatus.STOPPED);
                    cmdService.save(cmdItem);
                }
                return;
            }

            // kill current running cmd and update agent status from cmd callback
            for (Cmd runningCmd : getRunningCmd(target.getSessionId())) {
                Cmd killCmd = cmdService.create(new CmdInfo(runningCmd.getAgentPath(), CmdType.KILL, null));
                handler.get(CmdType.KILL).exec(killCmd);
            }

            target.setSessionId(null);
            agentService.saveWithStatus(target, AgentStatus.IDLE);
        }

        private List<Cmd> getRunningCmd(String sessionId) {
            List<Cmd> cmdsInSession = cmdService.listBySession(sessionId);

            Iterator<Cmd> iterator = cmdsInSession.iterator();
            while (iterator.hasNext()) {
                Cmd cmd = iterator.next();

                if (cmd.getType() != CmdType.RUN_SHELL) {
                    iterator.remove();
                    continue;
                }

                if (!cmd.isCurrent()) {
                    iterator.remove();
                }
            }

            return cmdsInSession;
        }
    }

    private class RunShellCmdHandler extends CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.RUN_SHELL;
        }

        /**
         * - Get agent from session
         * - Auto select agent by zone
         * - Get agent from zone and name
         */
        @Override
        Agent select(Cmd cmd) {
            if (cmd.hasSession()) {
                return agentService.find(cmd.getSessionId());
            }

            AgentPath path = cmd.getAgentPath();

            if (!path.hasName()) {
                return selectAgentFromZone(path.getZone());
            }

            return agentService.find(path);
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getAgentPath().toString());
            }

            // check agent status if without session
            if (!cmd.hasSession()) {
                if (!target.isAvailable()) {
                    throw new AgentErr.NotAvailableException(target.getName());
                }
                agentService.saveWithStatus(target, AgentStatus.BUSY);
            }

            sendCmdToAgent(target, cmd);
        }
    }

    private class SystemInfoCmdHandler extends CmdHandler {

        @Override
        CmdType handleType() {
            return CmdType.SYSTEM_INFO;
        }

        @Override
        Agent select(Cmd cmd) {
            return agentService.find(cmd.getAgentPath());
        }

        @Override
        void doExec(Agent target, Cmd cmd) {
            // do not deal with null target
            if (target == null) {
                return;
            }

            sendCmdToAgent(target, cmd);
        }
    }

    private class KillCmdHandler extends CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.KILL;
        }

        /**
         * Agent must be presented for kill cmd
         */
        @Override
        Agent select(Cmd cmd) {
            return agentService.find(cmd.getAgentPath());
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getAgentPath().toString());
            }

            sendCmdToAgent(target, cmd);
        }
    }

    private class StopCmdHandler extends CmdHandler {

        @Override
        public CmdType handleType() {
            return CmdType.STOP;
        }

        /**
         * Agent must be presented for stop cmd
         */
        @Override
        Agent select(Cmd cmd) {
            return agentService.find(cmd.getAgentPath());
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getAgentPath().toString());
            }

            if (target.getSessionId() != null) {
                handler.get(CmdType.DELETE_SESSION).exec(createDeleteSessionCmd(target));
            }

            // send stop cmd to agent
            sendCmdToAgent(target, cmd);

            // set agent to offline
            agentService.saveWithStatus(target, AgentStatus.OFFLINE);
        }
    }

    private class ShutdownCmdHandler extends CmdHandler {

        private final Logger logger = new Logger(ShutdownCmdHandler.class);

        @Override
        public CmdType handleType() {
            return CmdType.SHUTDOWN;
        }

        /**
         * Agent must be presented for shutdown cmd
         */
        @Override
        Agent select(Cmd cmd) {
            return agentService.find(cmd.getAgentPath());
        }

        @Override
        public void doExec(Agent target, Cmd cmd) {
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getAgentPath().toString());
            }

            // in shutdown action, cmd content is sudo password
            if (cmd.getCmd() == null) {
                throw new IllegalParameterException("For SHUTDOWN action, password of 'sudo' must be provided");
            }

            // delete session if session existed
            if (target.getSessionId() != null) {
                handler.get(CmdType.DELETE_SESSION).exec(createDeleteSessionCmd(target));
                logger.trace("Delete session before shutdown: %s %s", target.getPath(), target.getSessionId());
            }

            // otherwise kill cmd before shutdown
            else {
                Cmd killCmd = cmdService.create(new CmdInfo(target.getPath(), CmdType.KILL, null));
                handler.get(CmdType.KILL).exec(killCmd);
            }

            // set agent to offline
            agentService.saveWithStatus(target, AgentStatus.OFFLINE);
            logger.trace("Agent been shutdown: %s", target.getPath());
        }
    }
}
