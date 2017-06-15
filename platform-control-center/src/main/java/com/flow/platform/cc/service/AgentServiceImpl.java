package com.flow.platform.cc.service;

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.DateUtil;
import com.flow.platform.domain.*;
import com.flow.platform.util.logger.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl implements AgentService {

    private final static Logger LOGGER = new Logger(AgentService.class);

    // {zone : {path, agent}}
    private final Map<String, Map<AgentPath, Agent>> agentOnlineList = new HashMap<>();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private TaskConfig taskConfig;

    @Override
    public void reportOnline(String zone, Collection<AgentPath> keys) {
        onlineListUpdateLock.lock();
        try {
            Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());

            // find offline agent
            HashSet<AgentPath> offlines = new HashSet<>(agentList.keySet());
            offlines.removeAll(keys);

            // remote from online list and update status
            for (AgentPath key : offlines) {
                Agent offlineAgent = agentList.get(key);
                offlineAgent.setStatus(AgentStatus.OFFLINE);
                agentList.remove(key);
            }

            // fine newly online agent
            HashSet<AgentPath> onlines = new HashSet<>(keys);
            onlines.removeAll(agentList.keySet());

            // report online
            for (AgentPath key : onlines) {
                reportOnline(key);
            }
        } finally {
            onlineListUpdateLock.unlock();
        }
    }

    @Override
    public Agent find(AgentPath key) {
        String zone = key.getZone();
        Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
        return agentList.get(key);
    }

    @Override
    public Agent find(String sessionId) {
        //TODO: should replace with dao
        for (Zone zone : zoneService.getZones()) {
            Map<AgentPath, Agent> agentMap = agentOnlineList.get(zone.getName());
            if (agentMap == null) {
                continue;
            }

            for (Agent agent : agentMap.values()) {
                if (agent.getSessionId() != null && Objects.equals(agent.getSessionId(), sessionId)) {
                    return agent;
                }
            }
        }
        return null;
    }

    @Override
    public List<Agent> findAvailable(String zone) {
        Collection<Agent> onlines = onlineList(zone);

        // find available agent
        List<Agent> availableList = new LinkedList<>();
        for (Agent agent : onlines) {
            if (agent.getStatus() == AgentStatus.IDLE) {
                availableList.add(agent);
            }
        }

        // sort by update date, the first element is longest idle
        availableList.sort(Comparator.comparing(Agent::getUpdatedDate));
        return availableList;
    }

    @Override
    public Collection<Agent> onlineList(String zone) {
        Collection<Agent> zoneAgents = new ArrayList<>(agentOnlineList.size());
        Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
        for (Agent agent : agentList.values()) {
            if (agent.getZone().equals(zone)) {
                zoneAgents.add(agent);
            }
        }
        return zoneAgents;
    }

    @Override
    public void reportStatus(AgentPath path, AgentStatus status) {
        Agent exist = find(path);
        if (exist == null) {
            throw new AgentErr.NotFoundException(path.getName());
        }
        exist.setStatus(status);
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = AGENT_SESSION_TIMEOUT_TASK_PERIOD)
    public void sessionTimeoutTask() {
        if (!taskConfig.isEnableAgentSessionTimeoutTask()) {
            return;
        }

        // TODO: should be replaced by db query
        LOGGER.traceMarker("sessionTimeoutTask", "start");
        Date now = new Date();
        for (Zone zone : zoneService.getZones()) {
            Collection<Agent> agents = onlineList(zone.getName());
            for (Agent agent : agents) {
                if (agent.getSessionId() != null && isSessionTimeout(agent, now, AGENT_SESSION_TIMEOUT)) {
                    CmdBase cmd = new CmdBase(agent.getPath(), CmdType.DELETE_SESSION, null);
                    cmdService.send(cmd);
                }
            }
        }

        LOGGER.traceMarker("sessionTimeoutTask", "end");
    }

    public boolean isSessionTimeout(Agent agent, Date compareDate, long timeoutInSeconds) {
        if (agent.getSessionId() == null) {
            throw new UnsupportedOperationException("Target agent is not enable session");
        }

        ZonedDateTime utcDate = DateUtil.fromDateForUTC(compareDate);
        long sessionAlive = ChronoUnit.SECONDS.between(DateUtil.fromDateForUTC(agent.getSessionDate()), utcDate);

        return sessionAlive >= timeoutInSeconds;
    }

    /**
     * Find from online list and create
     *
     * @param key
     */
    private void reportOnline(AgentPath key) {
        Agent exist = find(key);
        if (exist == null) {
            String zone = key.getZone();
            Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
            Agent agent = new Agent(key);
            agent.setStatus(AgentStatus.IDLE);
            agentList.put(key, agent);
        }
    }
}
