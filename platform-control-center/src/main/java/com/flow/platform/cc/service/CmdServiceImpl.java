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

import static com.flow.platform.domain.CmdType.CREATE_SESSION;
import static com.flow.platform.domain.CmdType.DELETE_SESSION;
import static com.flow.platform.domain.CmdType.KILL;
import static com.flow.platform.domain.CmdType.RUN_SHELL;
import static com.flow.platform.domain.CmdType.SHUTDOWN;
import static com.flow.platform.domain.CmdType.STOP;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.dao.CmdResultDao;
import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.task.CmdWebhookTask;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkException.NotExitException;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author gy@fir.im
 */
@Service(value = "cmdService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class CmdServiceImpl implements CmdService {

    private final static Logger LOGGER = new Logger(CmdService.class);

    @Value("${mq.queue.cmd.name}")
    private String cmdQueueName;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private BlockingQueue<CmdStatusItem> cmdStatusQueue;

    @Autowired
    private CmdDao cmdDao;

    @Autowired
    private CmdResultDao cmdResultDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private RabbitTemplate cmdQueueTemplate;

    @Autowired
    protected ZKClient zkClient;

    private final Map<EnumSet<CmdType>, CmdValidator> cmdValidators = new HashMap<>();

    @PostConstruct
    public void init() {
        CmdValidatorForZoneRequired zoneRequired = new CmdValidatorForZoneRequired();
        cmdValidators.put(zoneRequired.targets(), zoneRequired);

        CmdValidatorForSessionRequired sessionRequired = new CmdValidatorForSessionRequired();
        cmdValidators.put(sessionRequired.targets(), sessionRequired);

        CmdValidatorForZoneAndNameRequired zoneAndNameRequired = new CmdValidatorForZoneAndNameRequired();
        cmdValidators.put(zoneAndNameRequired.targets(), zoneAndNameRequired);

        CmdValidatorForValidSession validSession = new CmdValidatorForValidSession();
        cmdValidators.put(validSession.targets(), validSession);
    }

    @Override
    public Cmd create(CmdInfo info) {
        Cmd cmd = Cmd.convert(info);
        cmd.setId(UUID.randomUUID().toString());
        cmd.setCreatedDate(ZonedDateTime.now());
        cmd.setUpdatedDate(ZonedDateTime.now());

        // validate input cmd
        for (Map.Entry<EnumSet<CmdType>, CmdValidator> entry : cmdValidators.entrySet()) {
            EnumSet<CmdType> types = entry.getKey();
            CmdValidator validator = entry.getValue();

            if (types.contains(cmd.getType())) {
                if (!validator.validate(cmd)) {
                    throw new IllegalParameterException("Invalid cmd: " + validator.errorMessage);
                }
            }
        }

        // auto create session id when create cmd
        if (cmd.getType() == CmdType.CREATE_SESSION) {
            cmd.setSessionId(UUID.randomUUID().toString());
            LOGGER.traceMarker("create", "Create session id when cmd created: %s", cmd.getSessionId());
        }

        if (cmd.getTimeout() == null) {
            cmd.setTimeout(DEFAULT_CMD_TIMEOUT);
        }

        return cmdDao.save(cmd);
    }

    @Override
    public Cmd find(String cmdId) {
        return cmdDao.get(cmdId);
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        return cmdDao.list(agentPath, null, null);
    }

    @Override
    public List<Cmd> listByZone(String zone) {
        return cmdDao.list(new AgentPath(zone, null), null, null);
    }

    @Override
    public List<CmdResult> listResult(Set<String> cmdIds) {
        return cmdResultDao.list(cmdIds);
    }

    @Override
    public boolean isTimeout(Cmd cmd) {
        if (cmd.getType() != CmdType.RUN_SHELL) {
            throw new IllegalParameterException("Check timeout only for run shell");
        }

        // not timeout since cmd is executed
        if (!cmd.isCurrent()) {
            return false;
        }

        ZonedDateTime createdAt = cmd.getCreatedDate();
        final long runningInSeconds = ChronoUnit.SECONDS.between(createdAt, ZonedDateTime.now());
        return runningInSeconds >= cmd.getTimeout();
    }

    @Override
    @Transactional(noRollbackFor = {FlowException.class, ZkException.class})
    public Cmd send(String cmdId, boolean shouldResetStatus) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalParameterException(String.format("Cmd '%s' does not exist", cmdId));
        }

        // verify input cmd status is in finished status
        if (!shouldResetStatus && !cmd.isCurrent()) {
            throw new IllegalStatusException(
                String.format("Cmd cannot be proceeded since status is: %s", cmd.getStatus()));
        }

        if (shouldResetStatus) {
            resetStatus(cmdId);
        }

        try {
            // find agent
            Agent target = selectAgent(cmd);
            LOGGER.traceMarker("send", "Agent been selected: %s with status %s", target.getPath(), target.getStatus());

            // set cmd path and save since some of cmd not defined agent name
            cmd.setAgentPath(target.getPath());
            cmdDao.update(cmd);

            // double check agent in zk node
            String agentNodePath = ZKHelper.buildPath(target.getPath());

            if (!zkClient.exist(agentNodePath)) {
                throw new AgentErr.NotFoundException(target.getPath().toString());
            }

            updateAgentStatusByCmdType(cmd, target);

            // set real cmd to zookeeper node
            if (cmd.isAgentCmd()) {
                zkClient.setData(agentNodePath, cmd.toBytes());
            }

            // update cmd status to SENT
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.SENT, null, false, true);
            updateStatus(statusItem, false);
            return cmd;

        } catch (AgentErr.NotAvailableException e) {
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            updateStatus(statusItem, false);
            zoneService.keepIdleAgentTask();
            throw e;

        } catch (NotExitException e) {
            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            updateStatus(statusItem, false);
            throw new AgentErr.NotFoundException(cmd.getAgentName());

        } catch (Throwable e) {
            CmdResult result = new CmdResult();
            result.getExceptions().add(e);

            CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            updateStatus(statusItem, false);
            throw e;
        }
    }

    @Override
    @Transactional(noRollbackFor = {FlowException.class, ZkException.class})
    public Cmd send(CmdInfo cmdInfo) {
        Cmd cmd = create(cmdInfo);
        return send(cmd.getId(), false);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Cmd queue(CmdInfo cmdInfo, int priority, int retry) {
        Cmd cmd = create(cmdInfo);

        CmdQueueItem item = new CmdQueueItem(cmd.getId(), priority, retry);
        MessageProperties properties = new MessageProperties();
        properties.setPriority(item.getPriority());
        cmdQueueTemplate.send("", cmdQueueName, new Message(item.toBytes(), properties));

        return cmd;
    }

    @Override
    public void updateStatus(CmdStatusItem statusItem, boolean inQueue) {
        if (inQueue) {
            try {
                LOGGER.trace("Report cmd status from queue: %s", statusItem.getCmdId());
                cmdStatusQueue.put(statusItem);
            } catch (InterruptedException ignore) {
                LOGGER.warn("Cmd status update queue warning");
            }
            return;
        }

        LOGGER.trace("Report cmd %s to status %s", statusItem.getCmdId(), statusItem.getStatus());
        String cmdId = statusItem.getCmdId();
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd does not exist");
        }

        CmdResult cmdResult = cmdResultDao.get(cmd.getId());

        // compare exiting cmd result and update
        CmdResult inputResult = statusItem.getCmdResult();
        if (inputResult != null) {
            inputResult.setCmdId(cmdId);
            cmd.setFinishedDate(inputResult.getFinishTime());
            if (cmdResult != null) {
                cmdResultDao.updateNotNullOrEmpty(inputResult);
            } else {
                cmdResultDao.save(inputResult);
            }
        }
        cmd.setCmdResult(cmdResult);

        // update cmd status
        if (cmd.addStatus(statusItem.getStatus())) {
            cmdDao.update(cmd);

            // update agent status
            if (statusItem.isUpdateAgentStatus()) {
                updateAgentStatusWhenUpdateCmd(cmd);
            }

            if (statusItem.isCallWebhook()) {
                webhookCallback(cmd);
            }
        }
    }

    @Override
    public void resetStatus(String cmdId) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd does not exist");
        }

        cmd.setStatus(CmdStatus.PENDING);
        cmdDao.save(cmd);
    }

    @Override
    public void saveLog(String cmdId, MultipartFile file) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path target = Paths.get(AppConfig.CMD_LOG_DIR.toString(), file.getOriginalFilename());
            Files.write(target, file.getBytes());
            cmd.getLogPaths().add(target.toString());
            cmdDao.update(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void webhookCallback(CmdBase cmdBase) {
        if (cmdBase == null || cmdBase.getWebhook() == null) {
            return;
        }

        taskExecutor.execute(new CmdWebhookTask(cmdBase));
    }

    @Scheduled(fixedDelay = 300 * 1000)
    public void checkTimeoutTask() {
        if (!taskConfig.isEnableCmdExecTimeoutTask()) {
            return;
        }
        LOGGER.traceMarker("checkTimeoutTask", "start");

        // find all running status cmd
        List<Cmd> cmds = cmdDao.list(null, Sets.newHashSet(CmdType.RUN_SHELL), Cmd.WORKING_STATUS);

        for (Cmd cmd : cmds) {
            if (isTimeout(cmd)) {
                // kill current running cmd and report status
                send(new CmdInfo(cmd.getAgentPath(), CmdType.KILL, null));
                LOGGER.traceMarker("checkTimeoutTask", "Send KILL for timeout cmd %s", cmd);

                // update cmd status via queue
                CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.TIMEOUT_KILL, null, true, true);
                updateStatus(statusItem, true);
            }
        }

        LOGGER.traceMarker("checkTimeoutTask", "end");
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
    private Agent selectAgent(CmdBase cmd) {
        // check session id as top priority
        if (cmd.hasSession()) {
            Agent target = agentService.find(cmd.getSessionId());
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getSessionId());
            }
            return target;
        }

        // verify agent path is presented
        AgentPath agentPath = cmd.getAgentPath();
        if (isAgentPathFail(cmd, agentPath)) {
            throw new AgentErr.AgentMustBeSpecified();
        }

        // auto select agent inside zone
        if (agentPath.getName() == null) {
            List<Agent> availableList = agentService.findAvailable(agentPath.getZone());
            if (availableList.size() > 0) {
                Agent target = availableList.get(0);
                cmd.setAgentPath(target.getPath()); // reset cmd path
                return target;
            }

            throw new AgentErr.NotAvailableException(agentPath.getZone() + ":null");
        }

        // find agent by path
        Agent target = agentService.find(agentPath);
        if (target == null) {
            throw new AgentErr.NotFoundException(cmd.getAgentName());
        }

        return target;
    }

    /**
     * Update agent status by cmd type
     *
     * @param cmd cmd instance created by cmdInfo
     * @param target target agent that needs to set status
     */
    private void updateAgentStatusByCmdType(final Cmd cmd, final Agent target) {
        switch (cmd.getType()) {
            case RUN_SHELL:
                if (cmd.hasSession()) {
                    break;
                }

                // add reject status since busy
                if (!target.isAvailable()) {
                    throw new AgentErr.NotAvailableException(target.getName());
                }

                target.setStatus(AgentStatus.BUSY);
                break;

            case CREATE_SESSION:

                // add reject status since unable to create session for agent
                String sessionId = agentService.createSession(target, cmd.getSessionId());
                if (sessionId == null) {
                    throw new AgentErr.NotAvailableException(target.getName());
                }

                break;

            case DELETE_SESSION:
                // send kill cmd to zookeeper
                String agentNodePath = ZKHelper.buildPath(target.getPath());
                CmdInfo killCmd = new CmdInfo(target.getPath(), CmdType.KILL, null);
                zkClient.setData(agentNodePath, killCmd.toBytes());

                agentService.deleteSession(target);
                break;

            case KILL:
                // DO NOT handle it, agent status from cmd update
                break;

            case STOP:
                agentService.deleteSession(target);
                target.setStatus(AgentStatus.OFFLINE);
                break;

            case SHUTDOWN:
                // in shutdown action, cmd content is sudo password
                if (Strings.isNullOrEmpty(cmd.getCmd())) {
                    throw new IllegalParameterException(
                        "For SHUTDOWN action, password of 'sudo' must be provided");
                }

                agentService.deleteSession(target);
                target.setStatus(AgentStatus.OFFLINE);
                break;
        }

        // update agent property
        LOGGER.debug("Target status record: %s %s", target.getPath(), target.getSessionId());
        agentDao.update(target);
    }

    private boolean isAgentPathFail(CmdBase cmd, AgentPath agentPath) {
        if (cmd.getType() == CmdType.CREATE_SESSION || cmd.getType() == CmdType.DELETE_SESSION) {
            return false;
        }
        return agentPath.getName() == null && cmd.getType() != CmdType.RUN_SHELL;
    }

    /**
     * Update agent status when report cmd status and result
     * - busy or idle by Cmd.Type.RUN_SHELL while report cmd status
     *
     * @param cmd Cmd object
     */
    private void updateAgentStatusWhenUpdateCmd(Cmd cmd) {
        // do not update agent status duration session
        String sessionId = cmd.getSessionId();
        if (sessionId != null && agentService.find(sessionId) != null) {
            return;
        }

        // update agent status by cmd status
        AgentPath agentPath = cmd.getAgentPath();
        boolean isAgentBusy = false;
        for (Cmd tmp : listByAgentPath(agentPath)) {
            if (tmp.getType() != CmdType.RUN_SHELL) {
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

        agentService.updateStatus(agentPath, isAgentBusy ? AgentStatus.BUSY : AgentStatus.IDLE);
    }

    /**
     * Interface to validate cmd is valid
     */
    private abstract class CmdValidator {

        protected final String ERR_MISSING_ZONE = "Missing agent zone definition";

        protected final String ERR_MISSING_NAME = "Missing agent name definition";

        abstract EnumSet<CmdType> targets();

        abstract boolean doValidation(Cmd cmd);

        protected String errorMessage;

        boolean validate(Cmd cmd) {
            if (!targets().contains(cmd.getType())) {
                errorMessage = "Cmd validator type does not match";
                return false;
            }

            // verify input cmd status is in finished status
            if (!cmd.isCurrent()) {
                errorMessage = "Cmd cannot be proceeded since status is: " + cmd.getStatus();
                return false;
            }

            AgentPath agentPath = cmd.getAgentPath();
            return agentPath != null && doValidation(cmd);
        }

        String error() {
            return errorMessage;
        }
    }

    /**
     * Path validator of only zone name required
     */
    private class CmdValidatorForZoneRequired extends CmdValidator {

        private final EnumSet<CmdType> targets = EnumSet.of(CREATE_SESSION, RUN_SHELL);

        @Override
        public EnumSet<CmdType> targets() {
            return targets;
        }

        @Override
        boolean doValidation(Cmd cmd) {
            if (Strings.isNullOrEmpty(cmd.getZoneName())) {
                errorMessage = ERR_MISSING_ZONE;
                return false;
            }

            Zone zone = zoneService.getZone(cmd.getZoneName());
            if (zone == null) {
                errorMessage = "Zone name not found: " + cmd.getZoneName();
                return false;
            }

            return true;
        }
    }

    private class CmdValidatorForSessionRequired extends CmdValidator {

        private final EnumSet<CmdType> targets = EnumSet.of(DELETE_SESSION);

        @Override
        public EnumSet<CmdType> targets() {
            return targets;
        }

        @Override
        boolean doValidation(Cmd cmd) {
            if (Strings.isNullOrEmpty(cmd.getSessionId())) {
                errorMessage = "Missing session id";
                return false;
            }

            return true;
        }
    }

    private class CmdValidatorForValidSession extends CmdValidator {

        private final EnumSet<CmdType> targets = EnumSet.of(DELETE_SESSION, RUN_SHELL);

        @Override
        EnumSet<CmdType> targets() {
            return targets;
        }

        @Override
        boolean doValidation(Cmd cmd) {
            final String sessionId = cmd.getSessionId();
            if (Strings.isNullOrEmpty(sessionId)) {
                return true;
            }

            Agent agent = agentService.find(sessionId);
            if (agent == null) {
                errorMessage = "Agent not found for session: " + sessionId;
                return false;
            }

            return true;
        }
    }

    private class CmdValidatorForZoneAndNameRequired extends CmdValidator {

        private final EnumSet<CmdType> targets = EnumSet.of(STOP, KILL, SHUTDOWN);

        @Override
        public EnumSet<CmdType> targets() {
            return targets;
        }

        @Override
        public boolean doValidation(Cmd cmd) {
            AgentPath path = cmd.getAgentPath();
            if (Strings.isNullOrEmpty(path.getZone())) {
                errorMessage = ERR_MISSING_ZONE;
                return false;
            }

            if (Strings.isNullOrEmpty(path.getName())) {
                errorMessage = ERR_MISSING_NAME;
                return false;
            }

            Agent agent = agentService.find(path);
            if (agent == null) {
                errorMessage = "Agent not found: " + cmd.getAgentPath();
                return false;
            }

            return true;
        }
    }
}