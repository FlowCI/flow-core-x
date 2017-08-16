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

import com.flow.platform.api.domain.Flow;

/**
 * To fetch related git repo info
 *
 * @author yang
 */
public interface GitService {

    // the folder in the flow workspace
    String SOURCE_FOLDER_NAME = "source";

    class Env {

        public final static String FLOW_GIT_SOURCE = "FLOW_GIT_SOURCE";

        public final static String FLOW_GIT_URL = "FLOW_GIT_URL";

        public final static String FLOW_GIT_BRANCH = "FLOW_GIT_BRANCH";

        public final static String FLOW_GIT_SSH_PRIVATE_KEY = "FLOW_GIT_SSH_PRIVATE_KEY";

        public final static String FLOW_GIT_SSH_PUBLIC_KEY = "FLOW_GIT_SSH_PUBLIC_KEY";

        public static final String FLOW_GIT_CHANGELOG = "FLOW_GIT_CHANGELOG";

        public static final String FLOW_GIT_EVENT_TYPE = "FLOW_GIT_EVENT_TYPE";
    }

    /**
     * Fetch file content from git repo in flow workspace
     */
    String fetch(Flow flow, String filePath);

    /**
     * Fetch file content from git repo by git clone
     *
     * @param flow flow instance which includes git repo info
     * @param filePath target file path
     * @return file content
     */
    String clone(Flow flow, String filePath);
}
