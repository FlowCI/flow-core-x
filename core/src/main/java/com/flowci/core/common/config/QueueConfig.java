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

package com.flowci.core.common.config;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
@Slf4j
@Configuration
public class QueueConfig {

    public static final String JobDlRoutingKey = "jobtimeout";

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Bean("rabbitTaskExecutor")
    public ThreadPoolTaskExecutor rabbitConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(20, 10, 100, "rabbit-task-");
    }

    @Bean
    public Connection rabbitConnection(ThreadPoolTaskExecutor rabbitTaskExecutor) throws Throwable {
        log.info("Rabbit URI: {}", rabbitProperties.getUri());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitProperties.getUri());
        factory.setRequestedHeartbeat(1800);

        return factory.newConnection(rabbitTaskExecutor.getThreadPoolExecutor());
    }

    @Bean("jobsQueueManager")
    public RabbitOperations jobsQueueManager(Connection rabbitConnection) throws IOException {
        RabbitOperations manager = new RabbitOperations(rabbitConnection, 1);

        // setup dead letter queue
        String queue = rabbitProperties.getJobDlQueue();
        manager.declare(queue, true);

        Map<String, Object> args = new HashMap<>(0);
        String exchange = rabbitProperties.getJobDlExchange();
        manager.declareExchangeAndBind(exchange, BuiltinExchangeType.DIRECT, true, false, args, queue, JobDlRoutingKey);

        return manager;
    }

    @Bean("idleAgentQueue")
    public String idleAgentQueue() {
        return "flow.idle.agent";
    }

    @Bean("idleAgentQueueManager")
    public RabbitOperations idleAgentQueueManager(Connection rabbitConnection, String idleAgentQueue) throws IOException {
        RabbitOperations manager = new RabbitOperations(rabbitConnection, 1);
        manager.declareTemp(idleAgentQueue);
        return manager;
    }

    @Bean("wsBroadcastQueue")
    public String wsBroadcastQueue() {
        return "bc.ws.q." + StringHelper.randomString(8);
    }

    @Bean
    public String eventBroadcastQueue() {
        return "bc.event.q." + StringHelper.randomString(8);
    }

    @Bean("broadcastQueueManager")
    public RabbitOperations broadcastQueueManager(Connection rabbitConnection,
                                                  String wsBroadcastQueue,
                                                  String eventBroadcastQueue) throws IOException {
        RabbitOperations manager = new RabbitOperations(rabbitConnection, 10);
        manager.declareTemp(wsBroadcastQueue);
        manager.declareExchangeAndBind(
                rabbitProperties.getWsBroadcastEx(),
                BuiltinExchangeType.FANOUT,
                wsBroadcastQueue,
                StringHelper.EMPTY
        );

        manager.declareTemp(eventBroadcastQueue);
        manager.declareExchangeAndBind(
                rabbitProperties.getEventBroadcastEx(),
                BuiltinExchangeType.FANOUT,
                eventBroadcastQueue,
                StringHelper.EMPTY
        );
        return manager;
    }
}
