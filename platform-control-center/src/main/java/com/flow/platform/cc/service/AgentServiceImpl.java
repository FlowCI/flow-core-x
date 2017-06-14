package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.DateUtil;
import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.dao.CmdDaoImpl;
import com.flow.platform.dao.CmdResultDaoImpl;
import com.flow.platform.domain.*;
import com.flow.platform.util.logger.Logger;
import com.flow.platform.util.mos.Instance;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl extends ZkServiceBase implements AgentService {

    private final static Logger LOGGER = new Logger(AgentService.class);

    // {zone : {path, agent}}
    private final Map<String, Map<AgentPath, Agent>> agentOnlineList = new HashMap<>();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private AgentDaoImpl agentDao;

    @Autowired
    private CmdDaoImpl cmdDao;

    @Autowired
    private CmdResultDaoImpl cmdResultDao;

    @PostConstruct
    public void init(){
        List<Agent> agents = agentDao.onlineList();
        for(Agent agent : agents){
            Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(agent.getZone(), k -> new HashMap<>());
            agentList.put(agent.getPath(), agent);
        }
    }

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

            // 这里的操作是一个zone下的所有的节点报上来，设置 online 还是 offline


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
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = KEEP_IDLE_AGENT_TASK_PERIOD)
    public void keepIdleAgentTask() {
        if (!AppConfig.ENABLE_KEEP_IDLE_AGENT_TASK) {
            return;
        }
        LOGGER.traceMarker("keepIdleAgentTask", "start");

        // get num of idle agent
        for (Zone zone : zoneService.getZones()) {
            InstanceManager instanceManager = zoneService.findInstanceManager(zone);
            if (instanceManager == null) {
                continue;
            }

            if (keepIdleAgentMinSize(zone, instanceManager, MIN_IDLE_AGENT_POOL)) {
                continue;
            }

            keepIdleAgentMaxSize(zone, instanceManager, MAX_IDLE_AGENT_POOL);
        }
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = AGENT_SESSION_TIMEOUT_TASK_PERIOD)
    public void sessionTimeoutTask() {
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
     * Find num of idle agent and batch start instance
     *
     * @param zone
     * @param instanceManager
     * @return boolean
     *          true = need start instance,
     *          false = has enough idle agent
     */
    public synchronized boolean keepIdleAgentMinSize(Zone zone, InstanceManager instanceManager, int minPoolSize) {
        int numOfIdle = this.findAvailable(zone.getName()).size();
        LOGGER.traceMarker("keepIdleAgentMinSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle < minPoolSize) {
            instanceManager.batchStartInstance(minPoolSize);
            return true;
        }

        return false;
    }

    /**
     * Find num of idle agent and check max pool size,
     * send shutdown cmd to agent and delete instance
     *
     * @param zone
     * @param instanceManager
     * @return
     */
    public synchronized boolean keepIdleAgentMaxSize(Zone zone, InstanceManager instanceManager, int maxPoolSize) {
        List<Agent> agentList = this.findAvailable(zone.getName());
        int numOfIdle = agentList.size();
        LOGGER.traceMarker("keepIdleAgentMaxSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle > maxPoolSize) {
            int numOfRemove = numOfIdle - maxPoolSize;

            for (int i = 0; i < numOfRemove; i++) {
                Agent idleAgent = agentList.get(i);

                // send shutdown cmd
                CmdBase cmd = new CmdBase(idleAgent.getPath(), CmdType.SHUTDOWN, "flow.ci");
                cmdService.send(cmd);

                // add instance to cleanup list
                Instance instance = instanceManager.find(idleAgent.getPath());
                if (instance != null) {
                    instanceManager.addToCleanList(instance);
                }
            }

            return true;
        }

        return false;
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
            agentDao.save(agent);
            agentList.put(key, agent);
        }
    }
}
