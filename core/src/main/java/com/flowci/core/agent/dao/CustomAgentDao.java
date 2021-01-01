package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.Agent;

import java.util.List;

public interface CustomAgentDao {

    long updateAllStatus(Agent.Status status);

    List<Agent> findAllWithoutTags();
}
