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
import com.flowci.core.common.manager.ResourceManager;
import com.flowci.core.flow.domain.Template;
import com.flowci.common.exception.StatusException;
import com.flowci.common.exception.UnsupportedException;
import com.flowci.tree.NodeTree;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yang
 */
@Slf4j
@Configuration
public class FlowConfig {

    @Autowired
    private AppProperties.Flow flowProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean("flowTreeCache")
    public Cache<String, NodeTree> flowTreeCache() {
        return CacheHelper.createLocalCache(50, 120);
    }

    @Bean("gitBranchCache")
    public Cache<String, List<String>> gitBranchCache() {
        return CacheHelper.createLocalCache(50, 300);
    }

    /**
     * Support two types resource file://xxxx and http[s]://
     */
    @Bean("templates")
    public List<Template> getTemplates(ResourceManager resourceManager) {
        var r = flowProperties.getTemplatesUrl();
        try {
            var typeRef = new TypeReference<List<Template>>() {
            };

            List<Template> list = objectMapper.readValue(resourceManager.getResource(r), typeRef);

            Set<String> titles = new HashSet<>(list.size());
            for (var t : list) {
                if (titles.contains(t.getTitle())) {
                    throw new UnsupportedException("Duplicated template title {0}", t.getTitle());
                }
                titles.add(t.getTitle());
            }

            log.info("Templates is loaded from {}", flowProperties.getTemplatesUrl());
            return list;
        } catch (Exception e) {
            throw new StatusException("Unable to load template from {0}", r.toString());
        }
    }

    @Bean("cronScheduler")
    public TaskScheduler concurrentTaskScheduler() {
        return new ConcurrentTaskScheduler();
    }
}
