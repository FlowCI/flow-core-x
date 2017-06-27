package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;

import java.util.List;

/**
 * Created by Will on 17/6/12.
 */
public interface AgentDao extends BaseDao<AgentPath, Agent> {

    /**
     * List agent by zone and status
     *
     * @param zone         target zone
     * @param orderByField the field should order by (date fields), null for createdDate
     * @param status       expect status list
     * @return list of agent
     */
    List<Agent> list(String zone, String orderByField, AgentStatus... status);

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
}
