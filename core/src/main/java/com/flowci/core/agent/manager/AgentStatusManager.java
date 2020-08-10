package com.flowci.core.agent.manager;

import com.flowci.domain.Agent;

public interface AgentStatusManager {

    Agent.Status get(Agent agent);

    void set(Agent agent, Agent.Status status);

    void delete(Agent agent);

    boolean isBusy(Agent agent);

    boolean isIdle(Agent agent);

    boolean isOffline(Agent agent);

    boolean isOnline(Agent agent);
}
