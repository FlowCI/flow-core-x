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

package com.flow.platform.cc.context;

import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.util.ZkHelper;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Listen spring application context refreshed event to initialize default zone
 *
 * @author gy@fir.im
 */
@Component
public class ContextInitHandler implements ApplicationListener<ContextRefreshedEvent> {

    private final static Logger LOGGER = new Logger(ContextInitHandler.class);

    @Autowired
    protected ZkHelper zkHelper;

    @Autowired
    private ZoneService zoneService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // init root node
        String path = zoneService.createRoot();
        LOGGER.trace("Root zookeeper node initialized: %s", path);

        // init zone nodes
        for (Zone zone : zkHelper.getDefaultZones()) {
            path = zoneService.createZone(zone);
            LOGGER.trace("Zone zookeeper node initialized: %s", path);
        }
    }
}
