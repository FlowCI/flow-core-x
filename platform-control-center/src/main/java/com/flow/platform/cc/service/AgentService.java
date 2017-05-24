package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentKey;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;

import java.util.Collection;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public interface AgentService {

    /**
     * Register agent when agent startup, which means report to online
     *
     * @param key AgentKey for zone and name info
     */
    void register(AgentKey key);

    /**
     * Batch register agent
     *
     * @param keys
     */
    void register(Collection<AgentKey> keys);

    /**
     * Find agent by zone and agent name
     *
     * @param key AgentKey object
     * @return Agent object, or null if not found
     */
    Agent find(AgentKey key);

    /**
     * Get online agent set by zone
     *
     * @param zone
     * @return
     */
    Collection<Agent> onlineAgent(String zone);

    /**
     * Update agent status
     *
     * @param agent source agent object
     * @param target target status
     */
    void statusChange(Agent agent, Agent.Status target);

    /**
     * Send ZkCmd to agent
     *
     * @param cmd
     * @return command objc with id
     * @exception AgentErr.NotAvailableException if agent busy
     */
    Cmd sendCommand(CmdBase cmd);
}
