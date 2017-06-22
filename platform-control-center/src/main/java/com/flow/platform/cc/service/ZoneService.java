package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.domain.Zone;

import java.util.List;

/**
 * Handle on zone level
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public interface ZoneService {

    int MIN_IDLE_AGENT_POOL = 1; // min pool size
    int MAX_IDLE_AGENT_POOL = 2; // max pool size
    int KEEP_IDLE_AGENT_TASK_PERIOD = 45 * 1000; // millisecond

    /**
     * Create zk node for agent zone
     *
     * @param zone
     * @return zk node path of zone
     */
    String createZone(Zone zone);

    /**
     * Find zone object by name
     *
     * @param zoneName
     * @return
     */
    Zone getZone(String zoneName);

    /**
     * Get zone list from zk
     *
     * @return zone list
     */
    List<Zone> getZones();

    /**
     * Find instance manager by zone name
     *
     * @param zone
     * @return
     */
    InstanceManager findInstanceManager(Zone zone);

    /**
     * Scheduler task, periodically, every 1 min to check available agent in zone
     * It will start instance if num of available agent not enough
     */
    void keepIdleAgentTask();
}
