package com.flow.platform.cc.service;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;

import java.util.Collection;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public interface AgentService {

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
    Collection<Agent> onlineList(String zone);

    /**
     * Find agent by zone and agent name from online list
     *
     * @param key AgentKey object
     * @return Agent object, or null if not found
     */
    Agent find(AgentPath key);

    /**
     * Update agent status
     *
     * @param path
     * @param status
     */
    void reportStatus(AgentPath path, Agent.Status status);
}
