package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;

import java.util.Collection;
import java.util.List;

/**
 * Created by Will on 17/6/12.
 */
public interface AgentDao {
    Collection<Agent> onlineList(String zone);
    Agent find(AgentPath agentPath);
    Agent findOnline(AgentPath agentPath);
    Agent find(String sessionId);
    Agent findOnline(String sessionId);
    List<Agent> findAvailable(String zone);
}
