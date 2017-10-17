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

import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.git.GitClientBuilder;
import com.flow.platform.api.git.GitHttpClientBuilder;
import com.flow.platform.api.git.GitLabClientBuilder;
import com.flow.platform.api.git.GitSshClientBuilder;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.UnsupportedException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class GitServiceImpl implements GitService {

    private class EmptyProgressListener implements ProgressListener {

        @Override
        public void onStart() {

        }

        @Override
        public void onStartTask(String task) {

        }

        @Override
        public void onProgressing(String task, int total, int progress) {

        }

        @Override
        public void onFinishTask(String task) {

        }
    }

    private final static Logger LOGGER = new Logger(GitService.class);

    private final Map<GitSource, Class<? extends GitClientBuilder>> clientBuilderType = new HashMap<>(6);

    @Autowired
    private Path workspace;

    @PostConstruct
    public void init() {
        clientBuilderType.put(GitSource.UNDEFINED_SSH, GitSshClientBuilder.class);
        clientBuilderType.put(GitSource.UNDEFINED_HTTP, GitHttpClientBuilder.class);
        clientBuilderType.put(GitSource.GITLAB, GitLabClientBuilder.class);
    }

    @Override
    public String fetch(Node node, String filePath, ProgressListener progressListener) throws GitException {
        GitClient client = gitClientInstance(node);

        if (progressListener == null) {
            progressListener = new EmptyProgressListener();
        }

        progressListener.onStart();
        String branch = node.getEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        return client.fetch(branch, filePath, new GitCloneProgressMonitor(progressListener));
    }

    @Override
    @Cacheable(value = "git.branches", key = "#node.getPath()", condition = "#refresh == false")
    public List<String> branches(Node node, boolean refresh) {
        GitClient client = gitClientInstance(node);
        try {
            return client.branches();
        } catch (GitException e) {
            throw new IllegalStatusException("Cannot load branch list from git: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "git.tags", key = "#node.getPath()", condition = "#refresh == false")
    public List<String> tags(Node node, boolean refresh) {
        GitClient client = gitClientInstance(node);
        try {
            return client.tags();
        } catch (GitException e) {
            throw new IllegalStatusException("Cannot load tag list from git: " + e.getMessage());
        }
    }

    @Override
    public GitCommit latestCommit(Node node) {
        GitClient client = gitClientInstance(node);
        String branch = node.getEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        try {
            return client.commit(branch);
        } catch (GitException e) {
            throw new IllegalStatusException("Cannot get latest commit data");
        }
    }

    private void checkRequiredEnv(Node node) {
        if (!EnvUtil.hasRequiredEnvKey(node, REQUIRED_ENVS)) {
            throw new IllegalParameterException("Missing required env variables");
        }
    }

    private static class GitCloneProgressMonitor implements ProgressMonitor {

        private final ProgressListener progressListener;

        private String currentTask;
        private int currentTotalWork;

        GitCloneProgressMonitor(ProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        @Override
        public void start(int totalTasks) {

        }

        @Override
        public void beginTask(String title, int totalWork) {
            currentTask = title;
            currentTotalWork = totalWork;
            progressListener.onStartTask(title);
        }

        @Override
        public void update(int completed) {
            progressListener.onProgressing(currentTask, currentTotalWork, completed);
        }

        @Override
        public void endTask() {
            progressListener.onFinishTask(currentTask);
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    /**
     * Init git client from flow env
     *
     * - FLOW_GIT_SOURCE
     * - FLOW_GIT_URL : UNDEFINED_HTTP / UNDEFINED_SSH
     * - FLOW_GIT_BRANCH
     * - FLOW_GIT_SSH_PRIVATE_KEY
     * - FLOW_GIT_SSH_PUBLIC_KEY
     * - FLOW_GIT_HTTP_USER
     * - FLOW_GIT_HTTP_PASS
     *
     * - FLOW_GITLAB_HOST
     * - FLOW_GITLAB_TOKEN
     * - FLOW_GITLAB_PROJECT
     */
    private GitClient gitClientInstance(Node node) {
        checkRequiredEnv(node);

        GitSource source = GitSource.valueOf(node.getEnv(GitEnvs.FLOW_GIT_SOURCE));
        Class<? extends GitClientBuilder> builderClass = clientBuilderType.get(source);
        if (builderClass == null) {
            throw new UnsupportedException(String.format("Git source %s not supported yet", source));
        }

        GitClientBuilder builder;
        try {
            builder = builderClass
                .getConstructor(Node.class, Path.class)
                .newInstance(node, gitSourcePath(node));
        } catch (Throwable e) {
            throw new IllegalStatusException("Fail to create GitClientBuilder instance: " + e.getMessage());
        }

        try {
            GitClient client = builder.build();
            LOGGER.trace("Git client initialized: %s", client);
            return client;
        } catch (GitException e) {
            throw new IllegalStatusException("Unable to init git client for " + source);
        }
    }

    /**
     * Get git source code folder path of flow workspace
     */
    private Path gitSourcePath(Node node) throws IOException {
        Path flowWorkspace = NodeUtil.workspacePath(workspace, node);
        Files.createDirectories(flowWorkspace);
        return Paths.get(flowWorkspace.toString(), SOURCE_FOLDER_NAME);
    }
}
