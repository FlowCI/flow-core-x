/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.service;

import com.flow.platform.cloud.InstanceManager;
import com.flow.platform.domain.Zone;
import java.util.List;

/**
 * @author gy@fir.im
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
