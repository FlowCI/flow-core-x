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
import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.dao.CmdLogDao;
import com.flow.platform.cc.dao.CmdResultDao;
import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.service.WebhookServiceImplBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdLog;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author gy@fir.im
 */

@Service
@Transactional
public class CmdServiceImpl extends WebhookServiceImplBase implements CmdService {

    private final static Logger LOGGER = new Logger(CmdService.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdStatusQueue;

    @Autowired
    private CmdDao cmdDao;

    @Autowired
    private CmdResultDao cmdResultDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdQueue;

    @Autowired
    private CmdLogDao cmdLogDao;

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
    @Transactional(noRollbackFor = Throwable.class)
    public Cmd create(CmdInfo info) {
        return create(info, 0);
    }

    @Override
    @Transactional(noRollbackFor = Throwable.class)
    public Cmd create(CmdInfo info, Integer retry) {
        Cmd cmd = Cmd.convert(info);
        cmd.setId(UUID.randomUUID().toString());
        cmd.setCreatedDate(ZonedDateTime.now());
        cmd.setUpdatedDate(ZonedDateTime.now());

        // check customized id from CmdInfo, therefore cmd id can be controlled by other system
        if (!Strings.isNullOrEmpty(info.getCustomizedId())) {
            cmd.setId(info.getCustomizedId());
        }

        // validate input cmd
        for (Map.Entry<EnumSet<CmdType>, CmdValidator> entry : cmdValidators.entrySet()) {
            EnumSet<CmdType> types = entry.getKey();
            CmdValidator validator = entry.getValue();

            if (types.contains(cmd.getType())) {
                validator.validate(cmd);
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

        if (retry != null && retry > 0) {
            cmd.setRetry(retry);
        }

        return cmdDao.save(cmd);
    }

    @Override
    public void save(Cmd cmd) {
        cmdDao.update(cmd);
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
    public List<Cmd> listBySession(String sessionId) {
        return cmdDao.list(sessionId);
    }

    @Override
    public List<CmdResult> listResult(Set<String> cmdIds) {
        return cmdResultDao.list(cmdIds);
    }

    @Override
    public List<Cmd> listWorkingCmd(AgentPath agentPath) {
        return cmdDao.list(agentPath, Sets.newHashSet(CmdType.RUN_SHELL), Cmd.WORKING_STATUS);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Cmd enqueue(CmdInfo cmdInfo, int priority, int retry) {
        Cmd cmd = create(cmdInfo, retry);

        CmdQueueItem item = new CmdQueueItem(cmd.getId(), retry);
        PriorityMessage message = PriorityMessage.create(item.toBytes(), priority);
        cmdQueue.enqueue(message);

        return cmd;
    }

    @Override
    public void updateStatus(CmdStatusItem statusItem, boolean inQueue) {
        if (inQueue) {
            LOGGER.trace("Report cmd status from queue: %s", statusItem.getCmdId());
            cmdStatusQueue.enqueue(PriorityMessage.create(statusItem.toBytes(), 1));
            return;
        }

        LOGGER.trace("Report cmd %s to status %s", statusItem.getCmdId(), statusItem.getStatus());
        String cmdId = statusItem.getCmdId();
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd does not exist");
        }

        //TODO: missing unit test
        // set cmd status in sequence
        if (!cmd.addStatus(statusItem.getStatus())) {
            LOGGER.warn("Cannot add cmd '%s' from '%s' status to '%s'",
                cmd.getId(), cmd.getStatus(), statusItem.getStatus());
            return;
        }

        // update cmd status
        save(cmd);

        // compare exiting cmd result and update
        CmdResult inputResult = statusItem.getCmdResult();

        if (inputResult != null) {
            inputResult.setCmdId(cmdId);
            cmdResultDao.saveOrUpdate(inputResult);
            cmd.setCmdResult(inputResult);
        }

        // update agent status
        if (statusItem.isUpdateAgentStatus()) {
            updateAgentStatusFromCmd(cmd);
        }

        if (statusItem.isCallWebhook()) {
            webhookCallback(cmd);
        }
    }

    @Override
    public void saveLog(String cmdId, MultipartFile file) {
        CmdLog cmdLog = cmdLogDao.get(cmdId);
        if (cmdLog == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path target = Paths.get(AppConfig.CMD_LOG_DIR.toString(), file.getOriginalFilename());
            Files.write(target, file.getBytes());
            cmdLog.setLogPath(target.toString());
            cmdLogDao.update(cmdLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update agent status when report cmd status and result
     * - DONOT update agent status if cmd with session, since it controlled by session cmd
     * - busy or idle by Cmd.Type.RUN_SHELL while report cmd status
     *
     * @param cmd Cmd object
     */
    private void updateAgentStatusFromCmd(Cmd cmd) {
        if (cmd.hasSession()) {
            return;
        }

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

        Agent agent = agentService.find(agentPath);
        agentService.saveWithStatus(agent, isAgentBusy ? AgentStatus.BUSY : AgentStatus.IDLE);
    }

    /**
     * Interface to validate cmd is valid
     */
    private abstract class CmdValidator {

        protected final String ERR_MISSING_ZONE = "Missing agent zone definition";

        protected final String ERR_MISSING_NAME = "Missing agent name definition";

        abstract EnumSet<CmdType> targets();

        abstract boolean doValidation(Cmd cmd);

        void validate(Cmd cmd) {
            if (!targets().contains(cmd.getType())) {
                throw new IllegalParameterException("Cmd validator type does not match");
            }

            // verify input cmd status is in finished status
            if (!cmd.isCurrent()) {
                throw new IllegalStatusException("Cmd cannot be proceeded since status is: " + cmd.getStatus());
            }

            AgentPath agentPath = cmd.getAgentPath();
            if (agentPath == null) {
                throw new IllegalParameterException("Cmd anget path does not set");
            }

            doValidation(cmd);
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
                throw new IllegalParameterException(ERR_MISSING_NAME);
            }

            Zone zone = zoneService.getZone(cmd.getZoneName());
            if (zone == null) {
                throw new IllegalParameterException("Zone name not found: " + cmd.getZoneName());
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
                throw new IllegalParameterException("Missing session id");
            }

            return true;
        }
    }

    private class CmdValidatorForValidSession extends CmdValidator {

        private final EnumSet<CmdType> targets = EnumSet.of(RUN_SHELL);

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
                throw new IllegalParameterException("Agent not found for session: " + sessionId);
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
                throw new IllegalParameterException(ERR_MISSING_ZONE);
            }

            if (Strings.isNullOrEmpty(path.getName())) {
                throw new IllegalParameterException(ERR_MISSING_NAME);
            }

            Agent agent = agentService.find(path);
            if (agent == null) {
                throw new AgentErr.NotFoundException("Agent not found: " + cmd.getAgentPath());
            }

            return true;
        }
    }
}