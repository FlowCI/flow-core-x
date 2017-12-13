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

package com.flow.platform.api.envs;

import java.util.Set;

/**
 * @author yang
 */
public enum JobEnvs implements EnvKey {

    FLOW_JOB_BUILD_NUMBER,

    /**
     * Job build category (trigger type)
     */
    FLOW_JOB_BUILD_CATEGORY,

    /**
     * Agent zone and name
     */
    FLOW_JOB_AGENT_INFO,

    /**
     * Job log download url
     */
    FLOW_JOB_LOG_PATH,

    /**
     * The last step status: values in {@code NodeStatus}
     */
    FLOW_JOB_LAST_STATUS,

    /**
     * The job cmd timeout in seconds
     */
    FLOW_JOB_CMD_TIMEOUT(false, true);

    private boolean readonly;

    private boolean editable;

    private Set<String> values;

    JobEnvs() {
        this.readonly = true;
        this.editable = false;
        this.values = null;
    }

    JobEnvs(boolean readonly, boolean editable) {
        this.readonly = readonly;
        this.editable = editable;
        this.values = null;
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public Set<String> availableValues() {
        return values;
    }
}
