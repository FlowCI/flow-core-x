package com.flow.platform.cc.service;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public interface AgentService {

    int AGENT_SESSION_TIMEOUT_TASK_PERIOD = 60 * 1000; // millisecond

    /**
     * Batch reportOnline agent
     *
     * @param zone
     * @param keys
     */
    void reportOnline(String zone, Collection<AgentPath> keys);

    /**
     * Get online agent set by zone
     *
     * @param zone
     * @return
     */
    List<Agent> onlineList(String zone);

    /**
     * Find agent by zone and agent name from online list
     *
     * @param key AgentKey object
     * @return Agent object, or null if not found
     */
    Agent find(AgentPath key);

    /**
     * FInd agent by session id
     *
     * @param sessionId
     * @return
     */
    Agent find(String sessionId);

    /**
     * Find available agent by zone name
     *
     * @param zone
     * @return Sorted agent list by updated date
     */
    List<Agent> findAvailable(String zone);

    /**
     * Create agent session
     *
     * @param agent
     * @return session id or null if unable to create session
     */
    String createSession(Agent agent);

    /**
     * Check has running cmd and delete agent session
     *
     * @param agent
     */
    void deleteSession(Agent agent);

    /**
     * Update agent status
     *
     * @param path
     * @param status
     */
    void updateStatus(AgentPath path, AgentStatus status);

    /**
     * Is agent session timeout
     *
     * @param agent
     * @param compareDate
     * @param timeoutInSeconds
     * @return
     */
    boolean isSessionTimeout(Agent agent, ZonedDateTime compareDate, long timeoutInSeconds);

    /**
     * To check agent session timeout
     */
    void sessionTimeoutTask();

}
