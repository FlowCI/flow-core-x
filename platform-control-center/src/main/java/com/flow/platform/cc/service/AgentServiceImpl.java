package com.flow.platform.cc.service;

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.dao.AgentDao;
import com.flow.platform.domain.*;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class AgentServiceImpl implements AgentService {

    private final static Logger LOGGER = new Logger(AgentService.class);

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private CountDownLatch initLatch;

    @PostConstruct
    public void init() {
        initLatch.countDown();
    }

    @Override
    public void reportOnline(String zone, Collection<AgentPath> keys) {
        List<Agent> agents = onlineList(zone);

        // convert to map for performance
        Map<AgentPath, Agent> onlineAgentMap = new HashMap<>(agents.size());
        for (Agent agent : agents) {
            onlineAgentMap.put(agent.getPath(), agent);
        }

        // find offline agents
        Set<AgentPath> onlineAgentKeys = onlineAgentMap.keySet();
        Set<AgentPath> offlines = Sets.newHashSet(onlineAgentKeys);
        offlines.removeAll(keys);

        // remove offline agents from online list and update status
        for (AgentPath key : offlines) {
            Agent offlineAgent = onlineAgentMap.get(key);
            offlineAgent.setStatus(AgentStatus.OFFLINE);
            agentDao.update(offlineAgent);
            onlineAgentMap.remove(key);
        }

        // report online
        for (AgentPath key : keys) {
            if (onlineAgentKeys.contains(key)) {
                continue;
            }
            reportOnline(key);
        }
    }

    @Override
    public Agent find(AgentPath key) {
        return agentDao.find(key);
    }

    @Override
    public Agent find(String sessionId) {
        return agentDao.find(sessionId);
    }

    @Override
    public List<Agent> findAvailable(String zone) {
        return agentDao.list(zone, "updatedDate", AgentStatus.IDLE);
    }

    @Override
    public List<Agent> onlineList(String zone) {
        return agentDao.list(zone, "createdDate", AgentStatus.IDLE, AgentStatus.BUSY);
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
    public String createSession(Agent agent) {
        if (!agent.isAvailable()) {
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        agent.setSessionId(sessionId); // set session id to agent
        agent.setSessionDate(ZonedDateTime.now());
        agent.setStatus(AgentStatus.BUSY);
        agentDao.update(agent);

        return sessionId;
    }

    @Override
    public void deleteSession(Agent agent) {
        boolean hasCurrentCmd = false;
        List<Cmd> agentCmdList = cmdService.listByAgentPath(agent.getPath());
        for (Cmd cmdItem : agentCmdList) {
            if (cmdItem.getType() == CmdType.RUN_SHELL && cmdItem.isCurrent()) {
                hasCurrentCmd = true;
                break;
            }
        }

        if (!hasCurrentCmd) {
            agent.setStatus(AgentStatus.IDLE);
        }

        agent.setSessionId(null); // release session from target
        agentDao.update(agent);
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

        // TODO: should be replaced by db query
        LOGGER.traceMarker("sessionTimeoutTask", "start");
        ZonedDateTime now = DateUtil.utcNow();

        for (Zone zone : zoneService.getZones()) {
            Collection<Agent> agents = onlineList(zone.getName());
            for (Agent agent : agents) {
                if (agent.getSessionId() != null
                    && isSessionTimeout(agent, now, zone.getAgentSessionTimeout())) {

                    CmdInfo cmdInfo = new CmdInfo(agent.getPath(), CmdType.DELETE_SESSION, null);
                    cmdService.send(cmdInfo);
                    LOGGER.traceMarker("sessionTimeoutTask", "Send DELETE_SESSION to agent %s",
                        agent);
                }
            }
        }

        LOGGER.traceMarker("sessionTimeoutTask", "end");
    }

    /**
     * Find from online list and create
     */
    private void reportOnline(AgentPath key) {
        Agent exist = find(key);

        // create new agent with idle status
        if (exist == null) {
            Agent agent = new Agent(key);
            agent.setStatus(AgentStatus.IDLE);
            agentDao.save(agent);
            return;
        }

        // update exist offline agent to idle status
        if (exist.getStatus() == AgentStatus.OFFLINE) {
            exist.setStatus(AgentStatus.IDLE);
            agentDao.update(exist);
        }
    }
}
