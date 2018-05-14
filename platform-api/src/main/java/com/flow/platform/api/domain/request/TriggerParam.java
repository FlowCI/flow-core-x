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
package com.flow.platform.api.domain.request;

import static com.flow.platform.api.envs.GitToggleEnvs.FLOW_GIT_PR_ENABLED;
import static com.flow.platform.api.envs.GitToggleEnvs.FLOW_GIT_PUSH_ENABLED;
import static com.flow.platform.api.envs.GitToggleEnvs.FLOW_GIT_PUSH_FILTER;
import static com.flow.platform.api.envs.GitToggleEnvs.FLOW_GIT_TAG_ENABLED;
import static com.flow.platform.api.envs.GitToggleEnvs.FLOW_GIT_TAG_FILTER;

import com.flow.platform.domain.Jsonable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author lhl
 */
@Data
public class TriggerParam {

    private List<String> branchFilter = new LinkedList<>();

    private List<String> tagFilter = new LinkedList<>();

    private boolean tagEnable = true;

    private boolean pushEnable = true;

    private boolean prEnable = true;

    public Map<String, String> toEnv() {
        HashMap<String, String> env = new HashMap<>();
        env.put(FLOW_GIT_PUSH_ENABLED.name(), Boolean.toString(pushEnable));
        env.put(FLOW_GIT_TAG_ENABLED.name(), Boolean.toString(tagEnable));
        env.put(FLOW_GIT_PR_ENABLED.name(), Boolean.toString(prEnable));

        env.put(FLOW_GIT_PUSH_FILTER.name(), Jsonable.GSON_CONFIG.toJson(branchFilter));
        env.put(FLOW_GIT_TAG_FILTER.name(), Jsonable.GSON_CONFIG.toJson(tagFilter));
        return env;
    }
}
