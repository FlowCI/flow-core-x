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

import com.flow.platform.cc.config.QueueConfig;
import com.flow.platform.cc.event.AgentResourceEvent;
import com.flow.platform.cc.event.AgentResourceEvent.Category;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.queue.PlatformQueue;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class AgentResourceEventHandler implements ApplicationListener<AgentResourceEvent> {

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdQueue;

    @Value("${queue.cmd.retry.enable}")
    private Boolean cmdQueueRetryEnable;

    @Override
    public void onApplicationEvent(AgentResourceEvent event) {
        String zone = event.getZone();
        log.trace("AgentResourceEvent received for zone '{}' with '{}'", zone, event.getCategory());

        // cleanup agent from zone
        zoneService.keepIdleAgentTask();

        // do not control cmd queue since enable retry
        Boolean isRetry = Boolean.parseBoolean(System.getProperty(QueueConfig.PROP_CMD_QUEUE_RETRY, "false"));
        if (cmdQueueRetryEnable || isRetry) {
            return;
        }

        if (event.getCategory() == Category.FULL) {
            cmdQueue.pause();
            log.trace("Pause cmd queue since no agent resources");
            return;
        }

        if (event.getCategory() == Category.RELEASED) {
            cmdQueue.resume();
            log.trace("Resume cmd queue since has agent resource released");
        }
    }
}
