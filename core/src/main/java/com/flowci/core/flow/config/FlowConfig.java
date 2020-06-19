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

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    private HttpClient httpClient;

    @Bean("gitTestExecutor")
    public ThreadPoolTaskExecutor gitTestExecutor() {
        return ThreadHelper.createTaskExecutor(20, 20, 100, "git-test-");
    }

    @Bean("gitBranchCache")
    public Cache<String, List<String>> gitBranchCache() {
        return CacheHelper.createLocalCache(50, 300);
    }

    @Bean("defaultTemplateYml")
    public String defaultTemplateYml() throws IOException {
        String url = flowProperties.getDefaultTemplateUrl();
        HttpResponse response = httpClient.execute(new HttpGet(url));
        String yml = EntityUtils.toString(response.getEntity());
        log.info("Default template yml is loaded from {}", url);
        return yml;
    }
}
