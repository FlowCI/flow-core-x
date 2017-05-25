package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;

import java.util.Collection;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public interface AgentService {

    /**
     * Batch reportOnline agent
     *
     * @param keys
     */
    void reportOnline(Collection<AgentPath> keys);

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
     * Send ZkCmd to agent
     *
     * @param cmd
     * @return command objc with id
     * @exception AgentErr.NotAvailableException if agent busy
     */
    Cmd sendCommand(CmdBase cmd);
}
