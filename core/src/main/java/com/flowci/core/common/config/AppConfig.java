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
import com.flowci.core.common.domain.SyncEvent;
import com.flowci.core.common.helper.JacksonHelper;
import com.flowci.util.FileHelper;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig {

    @Autowired
    private MultipartProperties multipartProperties;

    @Autowired
    private ConfigProperties appProperties;

    @PostConstruct
    private void initWorkspace() throws IOException {
        Path path = appProperties.getWorkspace();
        FileHelper.createDirectory(path);
        FileHelper.createDirectory(tmpDir());
    }

    @PostConstruct
    public void initUploadDir() throws IOException {
        Path path = Paths.get(multipartProperties.getLocation());
        FileHelper.createDirectory(path);
    }

    @Bean("serverUrl")
    public String serverUrl() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(appProperties.getUrl());
        String url = builder.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Bean("tmpDir")
    public Path tmpDir() {
        return Paths.get(appProperties.getWorkspace().toString(), "tmp");
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return JacksonHelper.create();
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster() {

            @Override
            public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
                if (event instanceof SyncEvent) {
                    ResolvableType type = (eventType != null ? eventType : ResolvableType.forInstance(event));
                    for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
                        invokeListener(listener, event);
                    }
                    return;
                }

                super.multicastEvent(event, eventType);
            }
        };

        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("s-event-");
        multicaster.setTaskExecutor(executor);
        return multicaster;
    }
}
