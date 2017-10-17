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

import com.flow.platform.api.domain.envs.EnvKey;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.model.GitCommit;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

/**
 * To fetch related git repo info
 *
 * @author yang
 */
public interface GitService {

    // the folder in the flow workspace
    String SOURCE_FOLDER_NAME = "source";

    Set<EnvKey> REQUIRED_ENVS = ImmutableSet.of(GitEnvs.FLOW_GIT_URL, GitEnvs.FLOW_GIT_SOURCE);

    interface ProgressListener {

        void onStart();

        void onStartTask(String task);

        void onProgressing(String task, int total, int progress);

        void onFinishTask(String task);
    }

    /**
     * Fetch file content from git repo by git clone
     *
     * @param node node instance which includes git repo info
     * @param filePath target file path in git repo
     * @param progress listener for git progress
     * @return file content
     */
    String fetch(Node node, String filePath, ProgressListener progress) throws GitException;

    /**
     * Fetch branches from git repo
     */
    List<String> branches(Node node);

    /**
     * Fetch tags from git repo
     */
    List<String> tags(Node node);

    /**
     * Fetch latest commit from git repo
     *
     * - For UNDEFINED_SSH or UNDEFINED_HTTP will be load from git local git repo, so use together with clone
     */
    GitCommit latestCommit(Node node);
}
