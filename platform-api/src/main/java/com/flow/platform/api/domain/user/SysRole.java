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
package com.flow.platform.api.domain.user;

import com.flow.platform.api.domain.permission.Actions;

/**
 * @author lhl
 */
public enum SysRole {

    ADMIN(Actions.values()),

    USER(new Actions[] {
        Actions.FLOW_YML,
        Actions.FLOW_SHOW,
        Actions.FLOW_CREATE,
        Actions.FLOW_SET_ENV,

        Actions.JOB_SHOW,
        Actions.JOB_LOG,
        Actions.JOB_CREATE,
        Actions.JOB_STOP,
        Actions.JOB_YML,

        Actions.GENERATE_KEY,
        Actions.CREDENTIAL_CREATE,

        Actions.AGENT_SHOW
    });

    private Actions[] actions;

    SysRole(Actions[] actions) {
        this.actions = actions;
    }

    public Actions[] getActions() {
        return actions;
    }
}
