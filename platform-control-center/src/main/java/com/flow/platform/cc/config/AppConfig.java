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

import com.flow.platform.core.config.AppConfigBase;
import com.flow.platform.core.config.DatabaseConfig;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author gy@fir.im
 */
@Configuration
@Import({DatabaseConfig.class, ZooKeeperConfig.class, QueueConfig.class, TaskConfig.class, AgentConfig.class})
public class AppConfig extends AppConfigBase {

    public final static String NAME = "Control Center";

    public final static String VERSION = "v0.1.3-alpha";

    private final static int ASYNC_POOL_SIZE = 100;

    private final static String THREAD_NAME_PREFIX = "async-task-";

    private final static Logger LOGGER = new Logger(AppConfig.class);

    private final static ThreadPoolTaskExecutor executor =
        ThreadUtil.createTaskExecutor(ASYNC_POOL_SIZE, ASYNC_POOL_SIZE / 10, 100, THREAD_NAME_PREFIX);

    @Value("${cc.workspace}")
    private String workspace;

    @Bean
    public Path workspace() {
        try {
            return Files.createDirectories(Paths.get(workspace));
        } catch (IOException e) {
            throw new RuntimeException("Fail to create flow.ci control center working dir", e);
        }
    }

    @Bean
    public Path cmdLogDir() {
        Path cmdLogDir = Paths.get(workspace().toString(), "agent-logs");
        try {
            return Files.createDirectories(cmdLogDir);
        } catch (IOException e) {
            throw new RuntimeException("Fail to create agent log dir", e);
        }
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        return eventMulticaster;
    }

    @Bean
    @Override
    public ThreadPoolTaskExecutor taskExecutor() {
        return executor;
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }
}
