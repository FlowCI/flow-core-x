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

package com.flow.platform.cc.consumer;

import com.flow.platform.cc.event.NoAvailableResourceEvent;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class NoAvailbleResourceEventHandler implements ApplicationListener<NoAvailableResourceEvent> {

    private final static Logger LOGGER = new Logger(NoAvailbleResourceEventHandler.class);

    @Autowired
    private ZoneService zoneService;

    @Override
    public void onApplicationEvent(NoAvailableResourceEvent event) {
        String zone = event.getZone();
        LOGGER.trace("Event received for zone: %s", zone);

        zoneService.keepIdleAgentTask();
    }
}
