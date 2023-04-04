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

package com.flowci.core.job.config;

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.job.domain.JobSmContext;
import com.flowci.core.job.domain.Step;
import com.flowci.sm.StateMachine;
import com.flowci.parser.v1.NodeTree;
import com.flowci.util.FileHelper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author yang
 */
@Slf4j
@Configuration
public class JobConfig {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ThreadPoolTaskExecutor appTaskExecutor;

    @Bean("jobTreeCache")
    public Cache<String, NodeTree> jobTreeCache() {
        return CacheHelper.createLocalCache(50, 120);
    }

    @Bean("jobStepCache")
    public Cache<String, List<Step>> jobStepCache() {
        return CacheHelper.createLocalCache(100, 60);
    }

    @Bean("repoDir")
    public Path pluginDir() throws IOException {
        String workspace = appProperties.getWorkspace().toString();
        Path pluginDir = Paths.get(workspace, "repos");
        return FileHelper.createDirectory(pluginDir);
    }

    @Bean("jobConditionExecutor")
    public ThreadPoolTaskExecutor jobConditionExecutor() {
        return ThreadHelper.createTaskExecutor(20, 20, 100, "job-cond-");
    }

    @Bean("sm")
    public StateMachine<JobSmContext> jobStateMachine() {
        return new StateMachine<>("JOB_STATUS", appTaskExecutor);
    }
}
