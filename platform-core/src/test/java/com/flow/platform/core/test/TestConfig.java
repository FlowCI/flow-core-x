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

package com.flow.platform.core.test;

import com.flow.platform.core.queue.MemoryQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.queue.PlatformQueue;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configurable
public class TestConfig {

    private final ThreadPoolTaskExecutor executor = ThreadUtil.createTaskExecutor(2, 2, 2, "test-executor");

    @Bean
    public ThreadPoolTaskExecutor executor() {
        return executor;
    }

    @Bean
    public PlatformQueue<PriorityMessage> inMemoryQueue() {
        return new MemoryQueue(executor, 1, "testInMemoryQueue");
    }

    @Bean
    public PlatformQueue<PriorityMessage> rabbitQueue() {
        return new RabbitQueue(executor, "amqp://localhost:5672", 1, 1, "ut-queue");
    }
}
