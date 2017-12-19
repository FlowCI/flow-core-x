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

import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.config.AppConfigBase;
import com.flow.platform.core.config.DatabaseConfig;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.plugin.PluginConfig;
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
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configuration
@Import({SchedulerConfig.class, CachingConfig.class, DatabaseConfig.class, QueueConfig.class, PluginConfig.class})
public class AppConfig extends AppConfigBase {

    public final static String NAME = "API";

    public final static String VERSION = "v0.1.3-alpha";

    public final static String DEFAULT_YML_FILE = ".flow.yml";

    public final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public final static String DEFAULT_AGENT_WORKSPACE = Paths.get("${HOME}", "flow-agent-workspace").toString();

    public final static String DEFAULT_AGENT_REPO_DIR = Paths.get("${HOME}", ".flow-agent", "repos").toString();

    private final static Logger LOGGER = new Logger(AppConfig.class);

    private final static int ASYNC_POOL_SIZE = 50;

    private final static String THREAD_NAME_PREFIX = "async-task-";

    private final static String MULTICASTER_THREAD_NAME_PREFIX = "multi_async-task-";

    private final static int MULTICASTER_ASYNC_POOL_SIZE = 1;

    private final static ThreadPoolTaskExecutor executor =
        ThreadUtil.createTaskExecutor(ASYNC_POOL_SIZE, ASYNC_POOL_SIZE / 10, 100, THREAD_NAME_PREFIX);

    private final static ThreadPoolTaskExecutor multicasterExecutor =
        ThreadUtil.createTaskExecutor(MULTICASTER_ASYNC_POOL_SIZE, MULTICASTER_ASYNC_POOL_SIZE, 1000,
            MULTICASTER_THREAD_NAME_PREFIX);

    @Value("${api.workspace}")
    private String workspace;

    @Value("${api.git.cache}")
    private String gitCloneCache;

    @Value("${api.git.workspace}")
    private String gitWorkspace;

    @Value("${domain.cc}")
    private String ccDomain;

    @Value(value = "${system.email}")
    private String email;

    @Value(value = "${system.username}")
    private String username;

    @Value(value = "${system.password}")
    private String password;

    @Bean
    public User superUser() {
        return new User(email, username, password);
    }

    @Bean
    public Path workspace() {
        try {
            return Files.createDirectories(Paths.get(workspace));
        } catch (IOException e) {
            throw new RuntimeException("Fail to create flow.ci api working dir", e);
        }
    }

    @Bean
    public Path gitWorkspace() {
        return Paths.get(gitWorkspace);
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        multicasterExecutor.initialize();
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(multicasterExecutor);
        return eventMulticaster;
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
        PlatformURL platformURL = new PlatformURL(ccDomain);
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
