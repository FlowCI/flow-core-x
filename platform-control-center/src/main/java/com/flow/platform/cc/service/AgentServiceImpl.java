package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Zone;
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

    private static final int MIN_IDLE_AGENT_POOL = 2; // min pool size
    private static final int MAX_IDLE_AGENT_POOL = 4; // max pool size

    private final Map<String, Map<AgentPath, Agent>> agentOnlineList = new HashMap<>();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private SpringContextUtil springContextUtil;

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
    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void keepIdleAgent() {
        if (!AppConfig.ENABLE_KEEP_IDLE_AGENT_TASK) {
            System.out.println("ZoneService.keepIdleAgent: Task not enabled");
            return;
        }

        // get num of idle agent
        for (Zone zone : zoneService.getZones()) {
            int numOfIdle = this.findAvailable(zone.getName()).size();
            System.out.println(String.format("Num of idle agent in zone %s = %s", zone.getName(), numOfIdle));

            // find instance manager by zone
            String beanName = String.format("%sInstanceManager", zone.getCloudProvider());
            InstanceManager instanceManager = (InstanceManager) springContextUtil.getBean(beanName);
            if (instanceManager == null) {
                continue;
            }

            // start batch of instance to ensure idle pool if size < min pool size
            if (numOfIdle < MIN_IDLE_AGENT_POOL) {
                instanceManager.batchStartInstance(MIN_IDLE_AGENT_POOL);
                continue;
            }

            // clean pool if idle pool size > max pool size
            List<Agent> idleList = this.findAvailable(zone.getName());
            numOfIdle = idleList.size();
            if (numOfIdle > MAX_IDLE_AGENT_POOL) {
                // TODO: clean instance when idle pool > max idle pool size
            }
        }
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
