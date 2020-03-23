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

import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Properties;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class FlowConfig {

    private static final Properties templateProperties = new Properties();

    static {
        templateProperties.setProperty("resource.loader", "class");
        templateProperties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        Velocity.init(templateProperties);
    }

    @Bean("gitTestExecutor")
    public ThreadPoolTaskExecutor gitTestExecutor() {
        return ThreadHelper.createTaskExecutor(20, 20, 100, "git-test-");
    }

    @Bean
    public Template defaultYmlTemplate() {
        return Velocity.getTemplate("templates/example.yml.vm");
    }

    @Bean("gitBranchCache")
    public Cache<String, List<String>> gitBranchCache() {
        return CacheHelper.createLocalCache(50, 300);
    }
}
