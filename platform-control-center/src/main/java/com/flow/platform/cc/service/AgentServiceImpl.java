package com.flow.platform.cc.service;

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.dao.CmdDaoImpl;
import com.flow.platform.dao.CmdResultDaoImpl;
import com.flow.platform.domain.*;
import org.hibernate.SessionFactory;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentDaoImpl agentDao;

    @Autowired
    private SpringContextUtil springContextUtil;

    public Map<String, Map<AgentPath, Agent>> getAgentOnlineList(){
        Map<String, Map<AgentPath, Agent>> agentOnline = new HashMap<>();
        List<Agent> agents = agentDao.onlineList();
        for(Agent agent : agents){
            Map<AgentPath, Agent> agentList = agentOnline.computeIfAbsent(agent.getZone(), k -> new HashMap<>());
            agentList.put(agent.getPath(), agent);
        }
        return agentOnline;
    }

    private TaskConfig taskConfig;

    @Override
    public void reportOnline(String zone, Collection<AgentPath> keys) {
        onlineListUpdateLock.lock();
        try {
            Map<AgentPath, Agent> agentList = getAgentOnlineList().computeIfAbsent(zone, k -> new HashMap<>());

            // find offline agent
            HashSet<AgentPath> offlines = new HashSet<>(agentList.keySet());
            offlines.removeAll(keys);

            // remote from online list and update status
            for (AgentPath key : offlines) {
                Agent offlineAgent = agentList.get(key);
                offlineAgent.setStatus(AgentStatus.OFFLINE);
                agentDao.update(offlineAgent);
                agentList.remove(key);
            }

            // fine newly online agent
            HashSet<AgentPath> onlines = new HashSet<>(keys);
            onlines.removeAll(agentList.keySet());

            // report online
            for (AgentPath key : onlines) {
                reportOnline(key);
            }
//
//            // 这里的操作是一个zone下的所有的节点报上来，设置 online 还是 offline
//

        } finally {
            onlineListUpdateLock.unlock();
        }
    }

    @Override
    public Agent find(AgentPath key) {
        //在所有的Agent中查询 并不是 online 的
        Agent agent  = agentDao.find(key);
        return agent;
    }

    @Override
    public Agent find(String sessionId) {
        return agentDao.find(sessionId);
    }

    @Override
    public List<Agent> findAvailable(String zone) {
//        Collection<Agent> onlines = onlineList(zone);
//
//        // find available agent
//        List<Agent> availableList = new LinkedList<>();
//        for (Agent agent : onlines) {
//            if (agent.getStatus() == AgentStatus.IDLE) {
//                availableList.add(agent);
//            }
//        }
//
//        // sort by update date, the first element is longest idle
//        availableList.sort(Comparator.comparing(Agent::getUpdatedDate));
        List<Agent> availableList = new LinkedList<>();
        availableList = agentDao.findAvailable(zone);
        return availableList;
    }

    @Override
    public Collection<Agent> onlineList(String zone) {
//        Collection<Agent> zoneAgents = new ArrayList<>(agentOnlineList.size());
//        Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
//        for (Agent agent : agentList.values()) {
//            if (agent.getZone().equals(zone)) {
//                zoneAgents.add(agent);
//            }
//        }
        Collection<Agent> zoneAgents = agentDao.onlineList(zone);
        return zoneAgents;
    }

    @Override
    public void reportStatus(AgentPath path, AgentStatus status) {
        Agent exist = find(path);
        if (exist == null) {
            throw new AgentErr.NotFoundException(path.getName());
        }
        exist.setStatus(status);
        agentDao.update(exist);
    }

    @Override
    public String createSession(Agent agent) {
        if(!agent.isAvailable()) {
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        agent.setSessionId(sessionId); // set session id to agent
        agent.setSessionDate(DateUtil.utcNow());
        agent.setStatus(AgentStatus.BUSY);
        // agent.save
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
                    LOGGER.traceMarker("sessionTimeoutTask", "Send DELETE_SESSION to agent %s", agent);
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
            Agent agent = new Agent(key);
            agent.setStatus(AgentStatus.IDLE);
            agentDao.save(agent);
        }
    }

    @Override
    public void setUnAutowiredInstance(AgentDaoImpl agentDao, ZoneService zoneService, CmdService cmdService) {
        this.agentDao = agentDao;
        this.zoneService = zoneService;
        this.cmdService = cmdService;
    }
}
