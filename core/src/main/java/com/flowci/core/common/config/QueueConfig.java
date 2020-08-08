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

import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@Configuration
public class QueueConfig {

    public static final String JobDlRoutingKey = "jobtimeout";

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Bean("rabbitTaskExecutor")
    public ThreadPoolTaskExecutor rabbitConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(10, 10, 50, "rabbit-task-");
    }

    @Bean
    public Connection rabbitConnection(ThreadPoolTaskExecutor rabbitTaskExecutor) throws Throwable {
        log.info("Rabbit URI: {}", rabbitProperties.getUri());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitProperties.getUri());
        factory.setRequestedHeartbeat(1800);

        return factory.newConnection(rabbitTaskExecutor.getThreadPoolExecutor());
    }

    @Bean("receiverQueueManager")
    public RabbitOperations receiverQueueManager(Connection rabbitConnection) throws IOException {
        String callbackQueue = rabbitProperties.getCallbackQueue();
        String shellLogQueue = rabbitProperties.getShellLogQueue();
        String ttyLogQueue = rabbitProperties.getTtyLogQueue();

        RabbitOperations manager = new RabbitOperations(rabbitConnection, 10);
        manager.declare(callbackQueue, true);
        manager.declare(shellLogQueue, true);
        manager.declare(ttyLogQueue, true);
        return manager;
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

    @Bean("agentQueueManager")
    public RabbitOperations agentQueueManager(Connection rabbitConnection) throws IOException {
        return new RabbitOperations(rabbitConnection, 1);
    }
}
