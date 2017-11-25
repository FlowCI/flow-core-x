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

package com.flow.platform.plugin.test;

import com.flow.platform.plugin.service.PluginService;
import com.flow.platform.plugin.service.PluginServiceImpl;
import com.flow.platform.plugin.service.PluginStoreService;
import com.flow.platform.plugin.service.PluginStoreServiceImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author gyfirim
 */

@Configuration
@ComponentScan({
    "com.flow.platform.plugin.consumer",
    "com.flow.platform.plugin.service"
})
public class AppConfig {

    private final static String PLUGIN_SOURCE_URL = "https://raw.githubusercontent.com/yunheli/plugins/master/repository.json";
    private final static String ERROR_PLUGIN_SOURCE_URL = "https://raw.githubusercontent.com/yunheli/plugins/master/repository_error.json";

    public Path folder;

    @PostConstruct
    protected void init() throws IOException {
        folder = Files.createDirectories(Paths.get("/tmp", "/test" + new Random(1000).toString()));
    }

    @PreDestroy
    protected void destroy() throws IOException {
        FileUtils.deleteDirectory(folder.toFile());
    }

    @Bean
    protected Path gitWorkspace() throws IOException {
        return Files.createDirectories(Paths.get(folder.toString(), "git-repo"));
    }

    @Bean
    protected Path gitCacheWorkspace() throws IOException {
        return Files.createDirectories(Paths.get(folder.toString(), "git-cache"));
    }

    @Bean
    protected Path workspace() {
        return folder;
    }

    @Bean
    protected ThreadPoolTaskExecutor pluginPoolExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    @Bean
    protected PluginService pluginService() {
        return new PluginServiceImpl();
    }

    @Bean
    protected PluginStoreService pluginStoreService() {
        return new PluginStoreServiceImpl();
    }

    @Bean
    protected String pluginSourceUrl() {
        return PLUGIN_SOURCE_URL;
    }
}
