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
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.util.StringHelper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
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
    private ConfigProperties.RabbitMQ rabbitProperties;

    @Bean
    public ThreadPoolTaskExecutor rabbitConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(10, 10, 50, "rabbit-t-");
    }

    @Bean
    public Connection rabbitConnection(ThreadPoolTaskExecutor rabbitConsumerExecutor) throws Throwable {
        log.info("Rabbit URI: {}", rabbitProperties.getUri());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitProperties.getUri());
        factory.setRequestedHeartbeat(1800);

        return factory.newConnection(rabbitConsumerExecutor.getThreadPoolExecutor());
    }

    @Bean
    public RabbitQueueOperation callbackQueueManager(Connection rabbitConnection) throws IOException {
        String name = rabbitProperties.getCallbackQueue();
        RabbitQueueOperation manager = new RabbitQueueOperation(rabbitConnection, 10, name);
        manager.declare(true);
        return manager;
    }

    @Bean
    public RabbitQueueOperation loggingQueueManager(Connection rabbitConnection) throws IOException {
        String name = rabbitProperties.getLoggingQueue();
        String exchange = rabbitProperties.getLoggingExchange();

        RabbitQueueOperation manager = new RabbitQueueOperation(rabbitConnection, 10, name);
        manager.declare(false);

        Channel channel = manager.getChannel();
        channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT);
        channel.queueBind(manager.getQueueName(), exchange, StringHelper.EMPTY);

        return manager;
    }

    @Bean
    public RabbitQueueOperation deadLetterQueueManager(Connection rabbitConnection) throws IOException {
        String name = rabbitProperties.getJobDlQueue();
        String exchange = rabbitProperties.getJobDlExchange();

        RabbitQueueOperation manager = new RabbitQueueOperation(rabbitConnection, 1, name);
        manager.declare(true);

        Map<String, Object> props = new HashMap<>(0);

        Channel channel = manager.getChannel();
        channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, true, false, props);
        channel.queueBind(manager.getQueueName(), exchange, JobDlRoutingKey);

        return manager;
    }

    @Bean
    public RabbitChannelOperation agentQueueManager(Connection rabbitConnection) throws IOException {
        return new RabbitChannelOperation(rabbitConnection, 1, "agent-channel-mgr");
    }
}
