/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.event.BroadcastEvent;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Log4j2
@Component("eventManager")
public class SpringEventManagerImpl implements SpringEventManager {

    private final static String HeaderClass = "CLASS";

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private String eventBroadcastQueue;

    @Autowired
    private RabbitOperations broadcastQueueManager;

    @PostConstruct
    public void subscribeBroadcastQueue() throws IOException {
        broadcastQueueManager.startConsumer(eventBroadcastQueue, true, (headers, body, envelope) -> {
            try {
                String className = headers.get(HeaderClass).toString();
                BroadcastEvent event = (BroadcastEvent) objectMapper.readValue(body, Class.forName(className));
                event.setInternal(true);
                publish(event);
            } catch (Exception e) {
                log.warn(e);
            }
            return false;
        }, null);
    }

    public <T extends ApplicationEvent> T publish(T event) {
        if (shouldBroadcast(event)) {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(event);
                Map<String, Object> header = ImmutableMap.of(HeaderClass, event.getClass().getName());
                this.broadcastQueueManager.sendToEx(rabbitProperties.getEventBroadcastEx(), bytes, header);
            } catch (JsonProcessingException e) {
                log.warn(e);
            }
            return event;
        }

        applicationEventPublisher.publishEvent(event);
        return event;
    }

    private static <T extends ApplicationEvent> boolean shouldBroadcast(T event) {
        if (event instanceof BroadcastEvent) {
            BroadcastEvent be = (BroadcastEvent) event;
            return !be.isInternal();
        }
        return false;
    }
}
