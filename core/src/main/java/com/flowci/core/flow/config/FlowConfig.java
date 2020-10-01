/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.manager.HttpRequestManager;
import com.flowci.core.flow.domain.Template;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.io.IOException;
import java.util.List;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class FlowConfig {

    @Autowired
    private AppProperties.Flow flowProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean("gitBranchCache")
    public Cache<String, List<String>> gitBranchCache() {
        return CacheHelper.createLocalCache(50, 300);
    }

    @Bean("templates")
    public List<Template> getTemplates(HttpRequestManager httpManager) throws IOException {
        String body = httpManager.get(flowProperties.getTemplatesUrl());

        TypeReference<List<Template>> typeRef = new TypeReference<List<Template>>() {
        };

        List<Template> list = objectMapper.readValue(body, typeRef);
        log.info("Templates is loaded from {}", flowProperties.getTemplatesUrl());
        return list;
    }

    @Bean("cronScheduler")
    public TaskScheduler concurrentTaskScheduler() {
        return new ConcurrentTaskScheduler();
    }
}
