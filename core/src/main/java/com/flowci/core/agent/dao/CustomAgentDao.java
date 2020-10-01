package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.Agent;

public interface CustomAgentDao {

    long updateAllStatus(Agent.Status status);
}
