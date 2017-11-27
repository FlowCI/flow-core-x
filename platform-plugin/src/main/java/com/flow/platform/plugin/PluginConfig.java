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

package com.flow.platform.plugin;

import com.flow.platform.util.http.HttpURL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author gyfirim
 */
@Configuration
@ComponentScan({
    "com.flow.platform.plugin.service",
    "com.flow.platform.plugin.consumer"
})
public class PluginConfig {

    private final static int QUEUE_MAX_SIZE = 100;

    private final static String PLUGIN_KEY = "plugin";

    private final static ThreadPoolTaskExecutor executor = initExecutor();

    @Value("${api.git.cache}")
    private String gitCloneCache;

    @Value("${api.git.workspace}")
    private String gitWorkspace;

    @Value("${plugins.repository}")
    private String pluginRepoUrl;

    @Bean
    public Path gitCacheWorkspace() {
        try {
            return Files.createDirectories(Paths.get(gitCloneCache));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Path gitWorkspace() {
        try {
            return Files.createDirectories(Paths.get(gitWorkspace));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public String pluginSourceUrl() {
        return HttpURL.build(pluginRepoUrl).toString();
    }

    @Bean
    public ThreadPoolTaskExecutor pluginPoolExecutor() {
        return executor;
    }

    private static ThreadPoolTaskExecutor initExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("plugin-");
        taskExecutor.setDaemon(true);
        return taskExecutor;
    }
}
