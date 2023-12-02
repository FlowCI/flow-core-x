/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.helper.FileHelper;
import com.flowci.core.common.event.AsyncEvent;
import com.flowci.core.common.helper.JacksonHelper;
import com.flowci.core.common.helper.ThreadHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * @author yang
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig {

    private final MultipartProperties multipartProperties;

    private final AppProperties appProperties;

    private final Path tmpDir;

    public AppConfig(MultipartProperties multipartProperties, AppProperties appProperties) {
        this.multipartProperties = multipartProperties;
        this.appProperties = appProperties;

        this.tmpDir = Paths.get(appProperties.getWorkspace().toString(), "tmp");
    }

    @PostConstruct
    private void initDirs() throws IOException {
        Path ws = appProperties.getWorkspace();
        FileHelper.createDirectory(ws);
        FileHelper.createDirectory(tmpDir);
        FileHelper.createDirectory(appProperties.getFlowDir());
        FileHelper.createDirectory(appProperties.getSiteDir());
    }

    @PostConstruct
    public void initUploadDir() throws IOException {
        Path path = Paths.get(multipartProperties.getLocation());
        FileHelper.createDirectory(path);
    }

    @Bean("tmpDir")
    public Path tmpDir() {
        return tmpDir;
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return JacksonHelper.Default;
    }

    @Bean("appTaskExecutor")
    public ThreadPoolTaskExecutor getAppTaskExecutor() {
        int corePoolSize = appProperties.getCorePoolSize();
        int maxPoolSize = appProperties.getMaxPoolSize();
        return ThreadHelper.createTaskExecutor(maxPoolSize, corePoolSize, 100, "app-task-");
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster(TaskExecutor appTaskExecutor) {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster() {

            private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
                return ResolvableType.forInstance(event);
            }

            @Override
            public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
                ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
                Executor executor = getTaskExecutor();
                for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
                    if (executor == null) {
                        invokeListener(listener, event);
                        continue;
                    }

                    if (listener instanceof AsyncEvent) {
                        executor.execute(() -> invokeListener(listener, event));
                        continue;
                    }

                    invokeListener(listener, event);
                }
            }
        };

        multicaster.setTaskExecutor(appTaskExecutor);
        return multicaster;
    }

    @Bean("httpClient")
    public HttpClient httpClient(TaskExecutor appTaskExecutor) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .executor(appTaskExecutor)
                .build();
    }
}
