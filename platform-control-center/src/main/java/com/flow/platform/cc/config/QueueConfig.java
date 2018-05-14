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

import com.flow.platform.core.queue.MemoryQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.queue.PlatformQueue;
import com.google.common.collect.Range;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@Configuration
public class QueueConfig {

    public final static int QUEUE_MAX_LENGTH = 100;

    /**
     * Default queue priority
     */
    public final static int DEFAULT_PRIORITY = 1;

    /**
     * The max queue priority for special case like retry
     */
    public final static int MAX_PRIORITY = 100;

    /**
     * The priority range for queue
     */
    public final static Range PRIORITY_RANGE = Range.closed(1, 10);

    public final static String PROP_CMD_QUEUE_RETRY = "queue.cmd.retry.enable";

    /**
     * Rabbit mq host
     * Example: amqp://guest:guest@localhost:5672
     */
    @Value("${mq.host}")
    private String host;

    /**
     * Rabbit mq management url
     * Example: http://localhost:15672
     */
    @Value("${mq.management.host}")
    private String mgrHost;

    /**
     * Cmd queue name for RabbitMQ
     */
    @Value("${queue.cmd.rabbit.name}")
    private String cmdQueueName;

    /**
     * Enable RabbitMQ or using embedded queue
     */
    @Value("${queue.cmd.rabbit.enable}")
    private Boolean cmdQueueRabbitEnable;

    /**
     * Enable cmd queue retry instead of pause/resume logic
     */
    @Value("${queue.cmd.retry.enable}")
    private Boolean cmdQueueRetryEnable;

    /**
     * AppConfig task executor
     */
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void init() {
        log.trace("Host: {}", host);
        log.trace("Management Host: {}", mgrHost);

        log.trace("Cmd queue name: {}", cmdQueueName);
        log.trace("Cmd RabbitMQ enabled: {}", cmdQueueRabbitEnable);
        log.trace("Cmd queue retry enabled: {}", cmdQueueRetryEnable);
    }

    @Bean
    public PlatformQueue<PriorityMessage> cmdQueue() {
        if (cmdQueueRabbitEnable) {
            log.trace("Apply RabbitMQ for cmd queue");
            return new RabbitQueue(taskExecutor, host, QUEUE_MAX_LENGTH, DEFAULT_PRIORITY, cmdQueueName);
        }

        log.trace("Apply in memory queue for cmd queue");
        return new MemoryQueue(taskExecutor, QUEUE_MAX_LENGTH, "CmdQueue");
    }

    /**
     * Queue to handle cmd status update
     */
    @Bean
    public PlatformQueue<PriorityMessage> cmdStatusQueue() {
        return new MemoryQueue(taskExecutor, QUEUE_MAX_LENGTH, "CmdStatusQueue");
    }
}