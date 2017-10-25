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

package com.flow.platform.api.consumer;

import com.flow.platform.api.config.WebSocketConfig;
import com.flow.platform.api.events.AgentStatusChangeEvent;
import com.flow.platform.api.push.PushHandler;
import com.flow.platform.domain.Agent;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
public class AgentStatusEventConsumer extends PushHandler implements ApplicationListener<AgentStatusChangeEvent> {

    @Override
    public void onApplicationEvent(AgentStatusChangeEvent event) {
        final Agent agent = event.getAgent();
        final String topic = String.format("%s/%s", WebSocketConfig.TOPIC_FOR_AGENT, agent.getPath().toString());
        System.out.println("onApplicationEvent:===================" + Thread.currentThread().getId());
        super.push(topic, agent);
    }
}
