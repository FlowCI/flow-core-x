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

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author gy@fir.im
 */
@Configuration
@Import({TaskConfig.class, MQConfig.class, DatabaseConfig.class, WebSocketConfig.class, AgentConfig.class})
public class AppConfig {

    public final static DateTimeFormatter APP_DATE_FORMAT = Jsonable.DOMAIN_DATE_FORMAT;

    public final static Path CMD_LOG_DIR = Paths.get(System.getenv("HOME"), "uploaded-agent-log");

    private final static int ASYNC_POOL_SIZE = 100;

    private final static Logger LOGGER = new Logger(AppConfig.class);


    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(CMD_LOG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: should separate task and scheduler
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(ASYNC_POOL_SIZE / 3);
        taskExecutor.setMaxPoolSize(ASYNC_POOL_SIZE);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("async-task-");
        return taskExecutor;
    }

    /**
     * Add cmd log file path to queue for processing such as upload log to cloud
     *
     * @return BlockingQueue with file path
     */
    @Bean
    public BlockingQueue<Path> cmdLoggingQueue() {
        return new LinkedBlockingQueue<>(50);
    }

    /**
     * Queue to handle agent report online in sync
     */
    @Bean
    public BlockingQueue<AgentPath> agentReportQueue() {
        return new LinkedBlockingQueue<>(50);
    }
}
