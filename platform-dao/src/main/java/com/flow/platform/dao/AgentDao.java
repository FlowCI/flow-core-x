package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;

import java.util.List;

/**
 * Created by Will on 17/6/12.
 */
public interface AgentDao {

    /**
     * List agent by zone and status
     *
     * @param zone   target zone
     * @param status expect status list
     * @return list of agent
     */
    List<Agent> list(String zone, AgentStatus... status);

    /**
     * Find agent by path
     *
     * @param agentPath AgentPath object
     * @return Agent instance
     */
    Agent find(AgentPath agentPath);

    /**
     * Find agent by session id
     *
     * @param sessionId session id
     * @return Agent instance
     */
    Agent find(String sessionId);

    void baseDelete(String condition);
}
