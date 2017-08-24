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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.git.GitClientBuilder;
import com.flow.platform.api.git.GitSshClientBuilder;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.UnsupportedException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostConstruct
    public void init() {
        clientBuilderType.put(GitSource.UNDEFINED_SSH, GitSshClientBuilder.class);
    }

    @Override
    public String clone(Flow flow, String filePath) throws GitException {
        String branch = flow.getEnv(GitEnvs.FLOW_GIT_BRANCH);
        GitClient client = gitClientInstance(flow);
        client.clone(branch, null, Sets.newHashSet(filePath));
        return fetch(flow, filePath);
    }

    /**
     * Init git client from flow env
     *
     * - FLOW_GIT_SOURCE
     * - FLOW_GIT_URL
     * - FLOW_GIT_BRANCH
     * - FLOW_GIT_SSH_PRIVATE_KEY
     * - FLOW_GIT_SSH_PUBLIC_KEY
     */
    private GitClient gitClientInstance(Flow flow) {
        GitSource source = GitSource.valueOf(flow.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Class<? extends GitClientBuilder> builderClass = clientBuilderType.get(source);
        if (builderClass == null) {
            throw new UnsupportedException(String.format("Git source %s not supported yet", source));
        }

        GitClientBuilder builder;
        try {
            builder = builderClass
                .getConstructor(Flow.class, Path.class)
                .newInstance(flow, gitSourcePath(flow));
        } catch (Throwable e) {
            throw new IllegalStatusException("Fail to create GitClientBuilder instance: " + e.getMessage());
        }

        GitClient client = builder.build();
        LOGGER.trace("Git client initialized: %s", client);
        return client;
    }

    /**
     * Get git source code folder path of flow workspace
     */
    private Path gitSourcePath(Flow flow) throws IOException {
        Path flowWorkspace = NodeUtil.workspacePath(workspace, flow);
        Files.createDirectories(flowWorkspace);
        return Paths.get(flowWorkspace.toString(), SOURCE_FOLDER_NAME);
    }

    /**
     * Get target file from local git repo folder
     */
    private String fetch(Flow flow, String filePath) {
        try {
            Path gitSourcePath = gitSourcePath(flow);
            Path targetPath = Paths.get(gitSourcePath.toString(), filePath);

            if (Files.exists(targetPath)) {
                return getContent(targetPath);
            }
        } catch (IOException warn) {
            LOGGER.warn("Fail to create git source dir for node: %s, %s", flow.getPath(), warn.getMessage());
        }

        return null;
    }

    /**
     * Get file content from source code folder of flow workspace
     */
    private String getContent(Path path) {
        try {
            return com.google.common.io.Files.toString(path.toFile(), AppConfig.DEFAULT_CHARSET);
        } catch (IOException e) {
            LOGGER.warn("Fail to load file content from %s", path.toString());
            return null;
        }
    }
}
