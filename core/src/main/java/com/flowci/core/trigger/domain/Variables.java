/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.trigger.domain;

public class Variables {

    /**
     * Git event source
     */
    public static final String GIT_SOURCE = "FLOWCI_GIT_SOURCE";

    /**
     * Git event type
     */
    public static final String GIT_EVENT = "FLOWCI_GIT_EVENT";

    /**
     * Email of the git user who start an event
     */
    public static final String GIT_AUTHOR = "FLOWCI_GIT_AUTHOR";

    public static final String GIT_BRANCH = "FLOWCI_GIT_BRANCH";

    public static final String GIT_COMMIT_ID = "FLOWCI_GIT_COMMIT_ID";

    public static final String GIT_COMMIT_MESSAGE = "FLOWCI_GIT_COMMIT_MESSAGE";

    public static final String GIT_COMMIT_TIME = "FLOWCI_GIT_COMMIT_TIME";

    public static final String GIT_COMMIT_URL = "FLOWCI_GIT_COMMIT_URL";

    public static final String GIT_COMMIT_NUM = "FLOWCI_GIT_COMMIT_NUM";

    /**
     * Variables for git pull request
     */
    public static final String PR_TITLE = "FLOWCI_GIT_PR_TITLE";

    public static final String PR_MESSAGE = "FLOWCI_GIT_PR_MESSAGE";

    public static final String PR_URL = "FLOWCI_GIT_PR_URL";

    public static final String PR_TIME = "FLOWCI_GIT_PR_TIME";

    public static final String PR_NUMBER = "FLOWCI_GIT_PR_NUMBER";

    public static final String PR_HEAD_REPO_NAME = "FLOWCI_GIT_PR_HEAD_REPO_NAME";

    public static final String PR_HEAD_REPO_BRANCH = "FLOWCI_GIT_PR_HEAD_REPO_BRANCH";

    public static final String PR_HEAD_REPO_COMMIT = "FLOWCI_GIT_PR_HEAD_REPO_COMMIT";

    public static final String PR_BASE_REPO_NAME = "FLOWCI_GIT_PR_BASE_REPO_NAME";

    public static final String PR_BASE_REPO_BRANCH = "FLOWCI_GIT_PR_BASE_REPO_BRANCH";

    public static final String PR_BASE_REPO_COMMIT = "FLOWCI_GIT_PR_BASE_REPO_COMMIT";

    private Variables() {

    }
}
