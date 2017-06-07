package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.domain.Zone;

import java.util.List;

/**
 * Handle on zone level
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public interface ZoneService {

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
}
