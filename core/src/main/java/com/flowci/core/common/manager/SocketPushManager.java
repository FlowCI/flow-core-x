/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.common.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.domain.PushBody;
import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.rabbit.RabbitOperations;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
@Log4j2
@Component
public class SocketPushManager {

    private final static String HeaderTopic = "TOPIC";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private String wsBroadcastQueue;

    @Autowired
    private RabbitOperations broadcastQueueManager;

    @EventListener(ContextRefreshedEvent.class)
    public void subscribeBroadcastQueue() throws IOException {
        broadcastQueueManager.startConsumer(wsBroadcastQueue, true, (headers, body, envelope) -> {
            try {
                String topic = headers.get(HeaderTopic).toString();
                simpMessagingTemplate.convertAndSend(topic, body);
            } catch (Exception e) {
                log.warn(e);
            }
            return false;
        }, null);
    }

    public void push(String topic, PushEvent event, Object obj) {
        try {
            PushBody push = new PushBody(event, obj);
            byte[] data = objectMapper.writeValueAsBytes(push);
            push(topic, data);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    public void push(String topic, byte[] bytes) {
        try {
            Map<String, Object> headers = new HashMap<>(1);
            headers.put(HeaderTopic, topic);
            broadcastQueueManager.sendToEx(rabbitProperties.getWsBroadcastEx(), bytes, headers);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
