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

import com.flow.platform.core.sysinfo.SystemInfo;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yh@firim
 */
@ComponentScan({
    "com.flow.platform.core.context",
    "com.flow.platform.core.service"
})
public abstract class AppConfigBase {

    public abstract SystemInfo systemInfo();

    protected ThreadPoolTaskExecutor taskExecutor(int asyncPoolSize, String threadNamePrefix) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(asyncPoolSize / 3);
        taskExecutor.setMaxPoolSize(asyncPoolSize);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix(threadNamePrefix);
        taskExecutor.setDaemon(true);
        return taskExecutor;
    }
}
