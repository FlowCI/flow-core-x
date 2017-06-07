package com.flow.platform.cc.service;

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
     * Get zone list from zk
     *
     * @return zone list
     */
    List<Zone> getZones();
}
