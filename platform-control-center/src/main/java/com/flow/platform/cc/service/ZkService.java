package com.flow.platform.cc.service;

import com.flow.platform.domain.Cmd;
import org.apache.zookeeper.Watcher;

import java.util.Set;

/**
 * Receive and process zookeeper event
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public interface ZkService extends Watcher {

    /**
     * Create zk node for agent zone
     *
     * @param zoneName
     * @return zk node path of zone
     */
    String createZone(String zoneName);

    /**
     * Get online agent set by zone
     *
     * @param zone
     * @return
     */
    Set<String> onlineAgent(String zone);

    /**
     * Send ZkCmd to agent
     *
     * @param zone
     * @param agent
     * @param cmd
     * @exception com.flow.platform.cc.exception.AgentErr.BusyException if agent busy
     */
    void sendCommand(String zone, String agent, Cmd cmd);
}
