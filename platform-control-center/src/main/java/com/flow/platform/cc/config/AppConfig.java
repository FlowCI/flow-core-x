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
import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.core.config.AppConfigBase;
import com.flow.platform.core.config.DatabaseConfig;
import com.flow.platform.core.sysinfo.PropertySystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * @author gy@fir.im
 */
@Configuration
@Import({
    DatabaseConfig.class,
    ZooKeeperConfig.class,
    MQConfig.class,
    TaskConfig.class,
    WebSocketConfig.class,
    AgentConfig.class
})
public class AppConfig extends AppConfigBase {

    public final static String NAME = "Control Center";

    public final static String VERSION = "alpha-0.1";

    public final static Path CMD_LOG_DIR = Paths.get(System.getenv("HOME"), "uploaded-agent-log");

    private final static int ASYNC_POOL_SIZE = 100;

    private final static String THREAD_NAME_PREFIX = "async-task-";

    private final static Logger LOGGER = new Logger(AppConfig.class);

    private final static ThreadPoolTaskExecutor executor =
        ThreadUtil.createTaskExecutor(ASYNC_POOL_SIZE, ASYNC_POOL_SIZE / 10, 100,THREAD_NAME_PREFIX);

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(CMD_LOG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        return eventMulticaster;
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        return executor;
    }

    /**
     * Queue to handle agent report online in sync
     */
    @Bean
    public BlockingQueue<AgentPath> agentReportQueue() {
        return new LinkedBlockingQueue<>(50);
    }

    /**
     * Queue to handle cmd status update
     */
    @Bean
    public BlockingQueue<CmdStatusItem> cmdStatusQueue() {
        return new LinkedBlockingQueue<>(50);
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
