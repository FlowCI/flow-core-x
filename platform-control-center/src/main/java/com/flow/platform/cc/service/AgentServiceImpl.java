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
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gy@fir.im
 */
@Service
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class AgentServiceImpl implements AgentService {

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
    private BlockingQueue<AgentPath> agentReportQueue;

    @Override
    public void reportOnline(String zone, Set<String> agents) {
        // set offline agents
        agentDao.batchUpdateStatus(zone, AgentStatus.OFFLINE, agents, true);

        // send to report queue
        for (String agent : agents) {
            AgentPath key = new AgentPath(zone, agent);
            try {

                agentReportQueue.put(key);
            } catch (InterruptedException ignore) {
                LOGGER.warn("InterruptedException when agent report online");
            }
        }
    }

    @Override
    public Agent find(AgentPath key) {
        return agentDao.get(key);
    }

    @Override
    public Agent find(String sessionId) {
        return agentDao.get(sessionId);
    }

    @Override
    public List<Agent> findAvailable(String zone) {
        return agentDao.list(zone, "updatedDate", AgentStatus.IDLE);
    }

    @Override
    public List<Agent> listForOnline(String zone) {
        return agentDao.list(zone, "createdDate", AgentStatus.IDLE, AgentStatus.BUSY);
    }

    @Override
    public List<Agent> list(String zone) {
        if (Strings.isNullOrEmpty(zone)) {
            return agentDao.list();
        }
        return listForOnline(zone);
    }

    @Override
    public void save(Agent agent) {
        agentDao.update(agent);
    }

    @Override
    public void updateStatus(AgentPath path, AgentStatus status) {
        Agent exist = find(path);
        if (exist == null) {
            throw new AgentErr.NotFoundException(path.getName());
        }
        exist.setStatus(status);
        agentDao.update(exist);
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
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = AGENT_SESSION_TIMEOUT_TASK_PERIOD)
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
                    cmdDispatchService.dispatch(delSessionCmd.getId(), false);
                    LOGGER.traceMarker("sessionTimeoutTask", "Send DELETE_SESSION to agent %s", agent);
                }
            }
        }

        LOGGER.traceMarker("sessionTimeoutTask", "end");
    }
}
