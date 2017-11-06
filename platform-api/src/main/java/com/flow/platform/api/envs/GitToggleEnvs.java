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

/**
 * @author yang
 */
public enum GitToggleEnvs implements EnvKey {

    /**
     * Enable or disable receive GIT PUSH event, value should be BOOLEAN
     */
    FLOW_GIT_PUSH_ENABLED,

    /**
     * Git push RE filter wish json format, ex: ["feature/api/*", "master"]
     */
    FLOW_GIT_PUSH_FILTER,

    /**
     * Enable or disable receive GIT TAG event, value should be BOOLEAN
     */
    FLOW_GIT_TAG_ENABLED,


    /**
     * Git push RE filter wish json format, ex: ["v1.0", "v2.0"]
     */
    FLOW_GIT_TAG_FILTER,

    /**
     * Enable or disable receive GIT PR event, value should be BOOLEAN
     */
    FLOW_GIT_PR_ENABLED


}
