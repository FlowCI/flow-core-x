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

package com.flow.platform.core.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yh@firim
 */
public abstract class AbstractAppConfig {
    private final static int ASYNC_POOL_SIZE = 100;

    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(ASYNC_POOL_SIZE / 3);
        taskExecutor.setMaxPoolSize(ASYNC_POOL_SIZE);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("async-task-");
        return taskExecutor;
    }
}
