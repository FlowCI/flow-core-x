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

package com.flowci.core.common.domain;

import com.google.common.collect.ImmutableList;

import java.util.Collection;

/**
 * @author yang
 */
public abstract class Variables {

    public abstract static class App {

        public static final String LogLevel = "FLOWCI_LOG_LEVEL";

        public static final String ServerUrl = "FLOWCI_SERVER_URL";

        public static final String Host = "FLOWCI_SERVER_HOST";

        public static final String ResourceDomain = "FLOWCI_RESOURCE_DOMAIN";
    }

    public abstract static class Flow {

        public static final String Name = "FLOWCI_FLOW_NAME";

        public static final String GitUrl = "FLOWCI_GIT_URL"; // set

        public static final String GitBranch = "FLOWCI_GIT_BRANCH"; // set

        public static final String GitRepo = "FLOWCI_GIT_REPO"; // set

        public static final String GitCredential = "FLOWCI_GIT_CREDENTIAL"; // set
    }

    public abstract static class Job {

        public static final String BuildNumber = "FLOWCI_JOB_BUILD_NUM";

        public static final String Trigger = "FLOWCI_JOB_TRIGGER";

        public static final String TriggerBy = "FLOWCI_JOB_TRIGGER_BY"; // == user email of job.createdBy

        public static final String StartAt = "FLOWCI_JOB_START_AT";

        public static final String FinishAt = "FLOWCI_JOB_FINISH_AT";

        public static final String DurationInSeconds = "FLOWCI_JOB_DURATION";

        // {step name}={status};{step name}={status}
        public static final String Steps = "FLOWCI_JOB_STEPS";

        // both status and error will carry out latest job status and error message from step
        public static final String Status = "FLOWCI_JOB_STATUS";

        public static final String Error = "FLOWCI_JOB_ERROR";
    }

    public abstract static class Step {

        // to control run step from docker defined in step or plugin, default is true
        public static final String DockerEnabled = "FLOWCI_STEP_DOCKER_ENABLED";
    }

    public abstract static class Agent {

        public static final String DockerImage = "FLOWCI_AGENT_IMAGE";

        public static final String ServerUrl = "FLOWCI_SERVER_URL";

        public static final String Token = "FLOWCI_AGENT_TOKEN";

        public static final String Workspace = "FLOWCI_AGENT_WORKSPACE";

        public static final String PluginDir = "FLOWCI_AGENT_PLUGIN_DIR";

        public static final String K8sEnabled = "FLOWCI_AGENT_K8S_ENABLED";

        public static final String K8sInCluster = "FLOWCI_AGENT_K8S_IN_CLUSTER";

        public static final String LogLevel = "FLOWCI_AGENT_LOG_LEVEL";

        public static final String Volumes = "FLOWCI_AGENT_VOLUMES";
    }

    public abstract static class Git {

        /**
         * Git event source
         */
        public static final String GIT_SOURCE = "FLOWCI_GIT_SOURCE";

        /**
         * Git event type
         */
        public static final String GIT_EVENT = "FLOWCI_GIT_EVENT";

        /**
         * Push / Tag variables
         */
        public static final String GIT_AUTHOR = "FLOWCI_GIT_AUTHOR";

        public static final String GIT_BRANCH = "FLOWCI_GIT_BRANCH";

        public static final String GIT_COMMIT_ID = "FLOWCI_GIT_COMMIT_ID";

        public static final String GIT_COMMIT_MESSAGE = "FLOWCI_GIT_COMMIT_MESSAGE";

        public static final String GIT_COMMIT_TIME = "FLOWCI_GIT_COMMIT_TIME";

        public static final String GIT_COMMIT_URL = "FLOWCI_GIT_COMMIT_URL";

        public static final String GIT_COMMIT_TOTAL = "FLOWCI_GIT_COMMIT_TOTAL";

        /**
         * b64 json content
         */
        public static final String GIT_COMMIT_LIST = "FLOWCI_GIT_COMMIT_LIST";

        public static final Collection<String> COMMIT_VARS = ImmutableList.<String>builder()
                .add(GIT_AUTHOR)
                .add(GIT_BRANCH)
                .add(GIT_COMMIT_ID)
                .add(GIT_COMMIT_MESSAGE)
                .add(GIT_COMMIT_TIME)
                .add(GIT_COMMIT_URL)
                .add(GIT_COMMIT_TOTAL)
                .add(GIT_COMMIT_LIST)
                .build();

        /**
         * Variables for git pull(mr) request
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

        public static final Collection<String> PR_VARS = ImmutableList.<String>builder()
                .add(PR_TITLE)
                .add(PR_MESSAGE)
                .add(PR_URL)
                .add(PR_TIME)
                .add(PR_NUMBER)
                .add(PR_HEAD_REPO_NAME)
                .add(PR_HEAD_REPO_BRANCH)
                .add(PR_HEAD_REPO_COMMIT)
                .add(PR_BASE_REPO_NAME)
                .add(PR_BASE_REPO_BRANCH)
                .add(PR_BASE_REPO_COMMIT)
                .build();

    }
}
