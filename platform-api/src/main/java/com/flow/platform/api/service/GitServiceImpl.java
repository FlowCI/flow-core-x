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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.git.GitClientBuilder;
import com.flow.platform.api.git.GitSshClientBuilder;
import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.exception.UnsupportedException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class GitServiceImpl implements GitService {

    private final static Logger LOGGER = new Logger(GitService.class);

    private final Map<GitSource, Class<? extends GitClientBuilder>> clientBuilderType = new HashMap<>(6);

    @Autowired
    private Path workspace;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void init() {
        clientBuilderType.put(GitSource.UNDEFINED_SSH, GitSshClientBuilder.class);
    }

    @Override
    public String fetch(Flow flow, String filePath) {
        GitClient client = gitClientInstance(flow);
        File gitFolder = client.clone(null, Sets.newHashSet(filePath));

        try {
            File targetFile = Paths.get(gitFolder.getParent(), filePath).toFile();
            if (targetFile.exists()) {
                return Files.toString(targetFile, Charset.forName("UTF-8"));
            }

            throw new NotFoundException("Target fetched file doesn't exist");
        } catch (IOException e) {
            throw new IllegalStatusException("Fail to read target fetched file");
        }
    }

    @Override
    public void fetch(Flow flow, String filePath, Consumer<String> callBack) {
        // TODO: progress of fetch may be included
        taskExecutor.execute(() -> {
            String content = fetch(flow, filePath);
            callBack.accept(content);
        });
    }

    /**
     * Init git client from flow env
     * - FLOW_GIT_SOURCE
     * - FLOW_GIT_URL
     * - FLOW_GIT_BRANCH
     * - FLOW_GIT_SSH_PRIVATE_KEY
     * - FLOW_GIT_SSH_PUBLIC_KEY
     */
    private GitClient gitClientInstance(Flow flow) {
        GitSource source = GitSource.valueOf(flow.getEnvs().get(Env.FLOW_GIT_SOURCE));
        Class<? extends GitClientBuilder> builderClass = clientBuilderType.get(source);
        if (builderClass == null) {
            throw new UnsupportedException(String.format("Git source %s not supported yet", source));
        }

        GitClientBuilder builder;
        try {
            Path flowWorkspace = flowDao.workspace(this.workspace, flow);
            builder = builderClass.getConstructor(Flow.class, Path.class).newInstance(flow, flowWorkspace);
        } catch (Throwable e) {
            throw new IllegalStatusException("Fail to create GitClientBuilder instance");
        }

        GitClient client = builder.build();
        LOGGER.trace("Git client initialized: %s", client);
        return client;
    }
}
