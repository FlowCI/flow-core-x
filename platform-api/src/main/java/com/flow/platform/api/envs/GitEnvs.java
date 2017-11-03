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
public enum GitEnvs implements EnvKey {

    /**
     * @see com.flow.platform.util.git.model.GitSource
     */
    FLOW_GIT_SOURCE(false, true, null),

    /**
     * For diff FLOW_GIT_SOURCE
     *  - UNDEFINED_SSH and UNDEFINED_HTTP is repo url
     *  - GITLAB is gitlab host url
     */
    FLOW_GIT_URL(false, true, null),

    FLOW_GIT_BRANCH(false, true, null),

    FLOW_GIT_WEBHOOK,

    FLOW_GIT_CHANGELOG,

    /**
     * @see com.flow.platform.util.git.model.GitEventType
     */
    FLOW_GIT_EVENT_TYPE,

    /**
     * @see com.flow.platform.util.git.model.GitSource
     */
    FLOW_GIT_EVENT_SOURCE,

    FLOW_GIT_COMMIT_ID,

    FLOW_GIT_COMMIT_URL, // http://git.com/xx/xxx

    FLOW_GIT_COMPARE_ID, // 123...456

    FLOW_GIT_COMPARE_URL, // http://github.com/xxx/123...456

    FLOW_GIT_PR_URL,

    FLOW_GIT_AUTHOR,

    FLOW_GIT_AUTHOR_EMAIL,

    /**
     * The credential name used for git
     */
    FLOW_GIT_CREDENTIAL(false, true, null),

    FLOW_GIT_SSH_PUBLIC_KEY,

    FLOW_GIT_SSH_PRIVATE_KEY,

    FLOW_GIT_HTTP_USER,

    FLOW_GIT_HTTP_PASS,

    /**
     * Env variables for GitLab
     */
    FLOW_GITLAB_TOKEN(true, false, null),

    FLOW_GITLAB_PROJECT(true, false, null);

    private boolean readonly;

    private boolean editable;

    private Set<EnvValue> values;

    /**
     * The default setting
     */
    GitEnvs() {
        this.readonly = true;
        this.editable = false;
        this.values = null;
    }

    GitEnvs(boolean readonly, boolean editable, Set<EnvValue> values) {
        this.readonly = readonly;
        this.editable = editable;
        this.values = values;
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
    public Set<EnvValue> availableValues() {
        return values;
    }
}
