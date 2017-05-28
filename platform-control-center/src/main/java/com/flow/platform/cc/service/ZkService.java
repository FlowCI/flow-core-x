package com.flow.platform.cc.service;

import org.apache.zookeeper.ZooKeeper;

/**
 * Receive and process zookeeper event
 *
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public interface ZkService {

    /**
     * Create zk node for agent zone
     *
     * @param zoneName
     * @return zk node path of zone
     */
    String createZone(String zoneName);

    /**
     * Get ZooKeeper instance
     *
     * @return ZooKeeper instance
     */
    ZooKeeper zkClient();

    /**
     * Get defined zones
     *
     * @return
     */
    String[] definedZones();
}
