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
public enum AgentEnvs implements EnvKey {

    FLOW_AGENT_WORKSPACE(false, true, null);

    private boolean readonly;

    private boolean editable;

    private Set<EnvValue> values;

    AgentEnvs(boolean readonly, boolean editable, Set<EnvValue> values) {
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
