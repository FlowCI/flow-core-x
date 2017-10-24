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

package com.flow.platform.api.config;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.config.AppConfigBase;
import com.flow.platform.core.config.DatabaseConfig;
import com.flow.platform.core.queue.InMemoryQueue;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.util.Logger;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configuration
@EnableAsync
@Import({CachingConfig.class, DatabaseConfig.class})
public class AppConfig extends AppConfigBase {

    public final static String NAME = "API";

    public final static String VERSION = "alpha-0.1";

    public final static String DEFAULT_YML_FILE = ".flow.yml";

    public final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private final static Logger LOGGER = new Logger(AppConfig.class);

    private final static int ASYNC_POOL_SIZE = 50;

    private final static String THREAD_NAME_PREFIX = "async-task-";
    private final static String DISPATCHER_THREAD_NAME_PREFIX = "async-task-dispatcher-";

    public final static String DEFAULT_USER_EMAIL = "admin@flow.ci";
    public final static String DEFAULT_USER_NAME = "admin";
    public final static String DEFAULT_USER_PASSWORD = "123456";

    private final static ThreadPoolTaskExecutor executor =
        ThreadUtil.createTaskExecutor(ASYNC_POOL_SIZE, ASYNC_POOL_SIZE / 10, 100, THREAD_NAME_PREFIX);

    private final static ThreadPoolTaskExecutor dispatcherExecutor =
        ThreadUtil.createTaskExecutor(1, 1, 1000, DISPATCHER_THREAD_NAME_PREFIX);


    @Value("${api.workspace}")
    private String workspace;

    @Value("${platform.url}")
    private String platFormBaseURL;

    @Bean
    public Path workspace() {
        try {
            Path dir = Files.createDirectories(Paths.get(workspace));
            LOGGER.trace("flow.ci working dir been created : %s", dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Fail to create flow.ci api working dir", e);
        }
    }

    @Bean
    public ThreadLocal<User> currentUser() {
        return new ThreadLocal<>();
    }

    @Bean
    public ThreadLocal<String> currentNodePath() {
        return new ThreadLocal<>();
    }

    @Bean
    @Override
    public ThreadPoolTaskExecutor taskExecutor() {
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor dispatcherExecutor(){
        return dispatcherExecutor;
    }

    /**
     * Queue to process cmd callback task
     */
    @Bean
    public PlatformQueue<CmdCallbackQueueItem> cmdCallbackQueue() {
        return new InMemoryQueue<>(executor, 50);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    @Bean
    public PlatformURL platformURL() {
        PlatformURL platformURL = new PlatformURL(platFormBaseURL);
        LOGGER.trace(platformURL.toString());
        return platformURL;
    }

    @Bean
    public VelocityEngine velocityEngine() throws Exception {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

        ve.setProperty(Velocity.ENCODING_DEFAULT, DEFAULT_CHARSET.name());
        ve.setProperty(Velocity.INPUT_ENCODING, DEFAULT_CHARSET.name());
        ve.setProperty(Velocity.OUTPUT_ENCODING, DEFAULT_CHARSET.name());

        ve.init();
        return ve;
    }
}
