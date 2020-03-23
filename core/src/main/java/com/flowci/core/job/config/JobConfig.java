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

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.NodeTree;
import com.flowci.util.FileHelper;
import com.github.benmanes.caffeine.cache.Cache;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class JobConfig {

    @Autowired
    private ConfigProperties appProperties;

    /**
     * Consume http request
     */
    @Bean("jobRunExecutor")
    public ThreadPoolTaskExecutor jobRunExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 100, "job-run-");
    }

    @Bean("jobDeleteExecutor")
    public ThreadPoolTaskExecutor jobDeleteExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 100, "job-delete-");
    }

    @Bean("jobTreeCache")
    public Cache<String, NodeTree> jobTreeCache() {
        return CacheHelper.createLocalCache(50, 60);
    }

    @Bean("jobStepCache")
    public Cache<String, List<ExecutedCmd>> jobStepCache() {
        return CacheHelper.createLocalCache(100, 60);
    }

    @Bean("repoDir")
    public Path pluginDir() throws IOException {
        String workspace = appProperties.getWorkspace().toString();
        Path pluginDir = Paths.get(workspace, "repos");
        return FileHelper.createDirectory(pluginDir);
    }
}
