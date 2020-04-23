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

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author yang
 */
public abstract class Variables {

    public abstract static class App {

        public static final String Url = "FLOWCI_SERVER_URL";

        public static final String Host = "FLOWCI_SERVER_HOST";

        public static final String RabbitHost = "FLOWCI_RABBIT_HOST";

        public static final String ZookeeperHost = "FLOWCI_ZOOKEEPER_HOST";

    }

    public abstract static class Flow {

        public static final String Name = "FLOWCI_FLOW_NAME";

        public static final String Webhook = "FLOWCI_FLOW_WEBHOOK";

        public static final String GitUrl = "FLOWCI_GIT_URL"; // set

        public static final String GitBranch = "FLOWCI_GIT_BRANCH"; // set

        public static final String GitCredential = "FLOWCI_GIT_CREDENTIAL"; // set
    }

    public abstract static class Job {

        public static final String BuildNumber = "FLOWCI_JOB_BUILD_NUM";

        public static final String Status = "FLOWCI_JOB_STATUS";

        public static final String Trigger = "FLOWCI_JOB_TRIGGER";

        public static final String TriggerBy = "FLOWCI_JOB_TRIGGER_BY"; // == user email of job.createdBy

        public static final String StartAt = "FLOWCI_JOB_START_AT";

        public static final String FinishAt = "FLOWCI_JOB_FINISH_AT";

        // {step name}={status};{step name}={status}
        public static final String Steps = "FLOWCI_JOB_STEPS";

        public static final List<String> Vars = Lists.newArrayList(
                BuildNumber,
                Status,
                Trigger,
                TriggerBy,
                StartAt,
                FinishAt,
                Steps
        );
    }

    public abstract static class Step {

        // to control run step from docker defined in step or plugin, default is true
        public static final String DockerEnabled = "FLOWCI_STEP_DOCKER_ENABLED";
    }
}
