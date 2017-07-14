package com.flow.platform.cc.service;

import com.flow.platform.cloud.InstanceManager;
import com.flow.platform.domain.Zone;

import java.util.List;

/**
 * Handle on zone level
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public interface ZoneService {

    int KEEP_IDLE_AGENT_TASK_PERIOD = 45 * 1000; // millisecond

    /**
     * Create zk root node
     *
     * @return zk root node path
     */
    String createRoot();

    /**
     * Create zk node for agent zone
     *
     * @return zk node path of zone
     */
    String createZone(Zone zone);

    /**
     * Find zone object by name
     */
    Zone getZone(String zoneName);

    /**
     * Get zone list from zk
     *
     * @return zone list
     */
    List<Zone> getZones();

    /**
     * Find instance manager by zone
     *
     * @param zone Zone instance
     * @return xxInstanceManager instance or Null
     */
    InstanceManager findInstanceManager(Zone zone);

    /**
     * Keep agent pool for min size
     */
    boolean keepIdleAgentMinSize(Zone zone, InstanceManager instanceManager);

    /**
     * Keep agent pool for max size
     */
    boolean keepIdleAgentMaxSize(Zone zone, InstanceManager instanceManager);

    /**
     * Scheduler task, periodically, every 1 min to check available agent in zone
     * It will start instance if num of available agent not enough
     */
    void keepIdleAgentTask();
}
