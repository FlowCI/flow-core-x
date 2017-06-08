package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.mos.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl extends ZkServiceBase implements AgentService {

    // {zone : {path, agent}}
    private final Map<String, Map<AgentPath, Agent>> agentOnlineList = new HashMap<>();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

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
                offlineAgent.setStatus(Agent.Status.OFFLINE);
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
            if (agent.getStatus() == Agent.Status.IDLE) {
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
    public void reportStatus(AgentPath path, Agent.Status status) {
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
            System.out.println("ZoneService.keepIdleAgentTask: Task not enabled");
            return;
        }

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
        System.out.println(String.format("Num of idle agent in zone %s = %s", zone, numOfIdle));

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
        System.out.println(String.format("Num of idle agent in zone %s = %s", zone, numOfIdle));

        if (numOfIdle > maxPoolSize) {
            int numOfRemove = numOfIdle - maxPoolSize;

            for (int i = 0; i < numOfRemove; i++) {
                Agent idleAgent = agentList.get(i);

                // send shutdown cmd
                CmdBase cmd = new CmdBase(idleAgent.getPath(), CmdBase.Type.SHUTDOWN, "flow.ci");
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
            agent.setStatus(Agent.Status.IDLE);
            agentList.put(key, agent);
        }
    }
}
