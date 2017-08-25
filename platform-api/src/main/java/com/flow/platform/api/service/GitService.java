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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.util.git.GitException;

/**
 * To fetch related git repo info
 *
 * @author yang
 */
public interface GitService {

    // the folder in the flow workspace
    String SOURCE_FOLDER_NAME = "source";

    interface ProgressListener {

        void onStart();

        void onStartTask(String task);

        void onProgressing(String task, int total, int progress);

        void onFinishTask(String task);

        void onFinish();
    }

    /**
     * Fetch file content from git repo by git clone
     *
     * @param node node instance which includes git repo info
     * @param filePath target file path in git repo
     * @param progress listener for git progress
     * @return file content
     */
    String clone(Node node, String filePath, ProgressListener progress) throws GitException;
}
