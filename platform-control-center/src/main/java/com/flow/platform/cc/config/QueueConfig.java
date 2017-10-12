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

package com.flow.platform.cc.config;

import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.core.queue.InMemoryQueue;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RabbitMQ configuration file
 *
 * @author gy@fir.im
 */
@Configuration
public class QueueConfig {

    public final static int CMD_QUEUE_MAX_LENGTH = 100;

    public final static int CMD_QUEUE_DEFAULT_PRIORITY = 10;

    private final static Logger LOGGER = new Logger(QueueConfig.class);

    @Value("${mq.host}")
    private String host; // amqp://guest:guest@localhost:5672

    @Value("${mq.management.host}")
    private String mgrHost; // http://localhost:15672

    @Value("${mq.queue.cmd.name}")
    private String cmdQueueName; // receive cmd from upstream

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor; // from AppConfig

    @PostConstruct
    public void init() {
        LOGGER.trace("Host: %s", host);
        LOGGER.trace("Management Host: %s", mgrHost);
        LOGGER.trace("Cmd queue name: %s", cmdQueueName);
    }

    @Bean
    public PlatformQueue<Message> cmdQueue() {
        return new RabbitQueue(taskExecutor, host, CMD_QUEUE_MAX_LENGTH, CMD_QUEUE_DEFAULT_PRIORITY, cmdQueueName);
    }

    /**
     * Queue to handle agent report online in sync
     */
    @Bean
    public PlatformQueue<AgentPath> agentReportQueue() {
        return new InMemoryQueue<>(taskExecutor, 100);
    }

    /**
     * Queue to handle cmd status update
     */
    @Bean
    public PlatformQueue<CmdStatusItem> cmdStatusQueue() {
        return new InMemoryQueue<>(taskExecutor, 100);
    }
}