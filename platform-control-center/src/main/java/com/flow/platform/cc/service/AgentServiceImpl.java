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
import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.event.AgentResourceEvent;
import com.flow.platform.cc.event.AgentResourceEvent.Category;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.service.WebhookServiceImplBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import java.sql.SQLDataException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gy@fir.im
 */
@Service
@Transactional
public class AgentServiceImpl extends WebhookServiceImplBase implements AgentService {

    private final static Logger LOGGER = new Logger(AgentService.class);

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private CmdDispatchService cmdDispatchService;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private AgentSettings agentSettings;

    @Override
    public void report(AgentPath path, AgentStatus status) {
        Agent exist = find(path);

        // For agent offline status
        if (status == AgentStatus.OFFLINE) {
            saveWithStatus(exist, AgentStatus.OFFLINE);
            return;
        }

        // create new agent with idle status
        if (exist == null) {
            try {
                exist = create(path, null);
                LOGGER.trace("Create agent %s from report", path);
            } catch (DataIntegrityViolationException ignore) {
                // agent been created at some other threads
                return;
            }
        }

        // update exist offline agent to idle status
        if (exist.getStatus() == AgentStatus.OFFLINE) {
            exist.setSessionId(null);
            saveWithStatus(exist, AgentStatus.IDLE);
        }

        // do not update agent status when its busy
        if (exist.getStatus() == AgentStatus.BUSY) {
            // do nothing
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(AgentPath key) {
        return agentDao.get(key);
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(String sessionId) {
        return agentDao.get(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> findAvailable(String zone) {
        return agentDao.list(zone, "updatedDate", AgentStatus.IDLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> listForOnline(String zone) {
        return agentDao.list(zone, "createdDate", AgentStatus.IDLE, AgentStatus.BUSY);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> list(String zone) {
        if (Strings.isNullOrEmpty(zone)) {
            return agentDao.list();
        }
        return agentDao.list(zone, "createdDate");
    }

    @Override
    public void saveWithStatus(Agent agent, AgentStatus status) {
        if (agent == null || status == null) {
            return;
        }

        if (!agentDao.exist(agent.getPath())) {
            throw new AgentErr.NotFoundException(agent.getName());
        }

        boolean statusIsChanged = !agent.getStatus().equals(status);

        agent.setStatus(status);
        agentDao.update(agent);
        LOGGER.trace("Agent status been updated to '%s'", status);

        // send webhook if status changed
        if (statusIsChanged) {
            this.webhookCallback(agent);
        }

        // boardcast AgentResourceEvent for release
        if (agent.getStatus() == AgentStatus.IDLE) {
            this.dispatchEvent(new AgentResourceEvent(this, agent.getZone(), Category.RELEASED));
        }
    }

    @Override
    public boolean isSessionTimeout(Agent agent, ZonedDateTime compareDate, long timeoutInSeconds) {
        if (agent.getSessionId() == null) {
            throw new UnsupportedOperationException("Target agent is not enable session");
        }

        long sessionAlive = ChronoUnit.SECONDS.between(agent.getSessionDate(), compareDate);
        return sessionAlive >= timeoutInSeconds;
    }

    @Override
    public Agent create(AgentPath agentPath, String webhook) {
        Agent agent = agentDao.get(agentPath);
        if (agent != null) {
            throw new IllegalParameterException(String.format("The agent '%s' has already exsited", agentPath));
        }

        agent = new Agent(agentPath);
        agent.setCreatedDate(DateUtil.now());
        agent.setUpdatedDate(DateUtil.now());
        agent.setStatus(AgentStatus.OFFLINE);
        agent.setWebhook(webhook);

        //random token
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);

        return agent;
    }

    @Override
    public String refreshToken(AgentPath agentPath) {
        Agent agent = agentDao.get(agentPath);
        if (agent != null) {
            throw new IllegalParameterException(String.format("The agent '%s' has already exsited", agentPath));
        }

        //random token
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);

        return agent.getToken();
    }

    @Override
    public AgentSettings settings(String token) {
        Agent agent = agentDao.getByToken(token);

        // validate token
        if (agent == null) {
            throw new IllegalParameterException("Illegal agent token");
        }

        agentSettings.setAgentPath(agent.getPath());
        return agentSettings;
    }

    @Override
    public void delete(Agent agent) {
        try {
            agentDao.delete(agent);
        } catch (Throwable e) {
            throw new UnsupportedOperationException("delete agent failure " + e.getMessage());
        }

    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = SESSION_TIMEOUT_TASK_HEARTBEAT)
    public void sessionTimeoutTask() {
        if (!taskConfig.isEnableAgentSessionTimeoutTask()) {
            return;
        }

        LOGGER.traceMarker("sessionTimeoutTask", "start");
        ZonedDateTime now = DateUtil.utcNow();

        for (Zone zone : zoneService.getZones()) {
            Collection<Agent> agents = listForOnline(zone.getName());
            for (Agent agent : agents) {
                if (agent.getSessionId() != null && isSessionTimeout(agent, now, zone.getAgentSessionTimeout())) {
                    Cmd delSessionCmd = cmdService.create(new CmdInfo(agent.getPath(), CmdType.DELETE_SESSION, null));
                    cmdDispatchService.dispatch(delSessionCmd);
                    LOGGER.traceMarker("sessionTimeoutTask", "Send DELETE_SESSION to agent %s", agent);
                }
            }
        }

        LOGGER.traceMarker("sessionTimeoutTask", "end");
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = IDLE_AGENT_TASK_HEARTBEAT)
    public void idleAgentTask() {
        for (Zone zone : zoneService.getZones()) {
            List<Agent> availableList = findAvailable(zone.getName());
            if (availableList.size() > 0) {
                this.dispatchEvent(new AgentResourceEvent(this, zone.getName(), Category.RELEASED));
            }
        }
    }
}
