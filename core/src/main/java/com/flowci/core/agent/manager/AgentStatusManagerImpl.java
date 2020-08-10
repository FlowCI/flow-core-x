package com.flowci.core.agent.manager;

import com.flowci.domain.Agent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentStatusManagerImpl implements AgentStatusManager {

    private final Map<String, Agent.Status> statusMap = new ConcurrentHashMap<>();

    @Override
    public Agent.Status get(Agent agent) {
        return statusMap.getOrDefault(agent.getId(), Agent.Status.OFFLINE);
    }

    @Override
    public void set(Agent agent, Agent.Status status) {
        statusMap.put(agent.getId(), status);
    }

    @Override
    public void delete(Agent agent) {
        statusMap.remove(agent.getId());
    }

    @Override
    public boolean isBusy(Agent agent) {
        return statusMap.getOrDefault(agent.getId(), Agent.Status.OFFLINE) == Agent.Status.BUSY;
    }

    @Override
    public boolean isIdle(Agent agent) {
        return statusMap.getOrDefault(agent.getId(), Agent.Status.OFFLINE) == Agent.Status.IDLE;
    }

    @Override
    public boolean isOffline(Agent agent) {
        return statusMap.getOrDefault(agent.getId(), Agent.Status.OFFLINE) == Agent.Status.OFFLINE;
    }

    @Override
    public boolean isOnline(Agent agent) {
        return !isOffline(agent);
    }
}
