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

import com.flow.platform.api.envs.GitToggleEnvs;
import com.flow.platform.domain.Jsonable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author lhl
 */
public class TriggerParam {

    private List<String> branchFilter = new LinkedList<>();

    private List<String> tagFilter = new LinkedList<>();

    private boolean tagEnable = true;

    private boolean pushEnable = true;

    private boolean prEnable = true;

    public List<String> getBranchFilter() {
        return branchFilter;
    }

    public void setBranchFilter(List<String> branchFilter) {
        this.branchFilter = branchFilter;
    }

    public List<String> getTagFilter() {
        return tagFilter;
    }

    public void setTagFilter(List<String> tagFilter) {
        this.tagFilter = tagFilter;
    }

    public boolean isTagEnable() {
        return tagEnable;
    }

    public void setTagEnable(boolean tagEnable) {
        this.tagEnable = tagEnable;
    }

    public boolean isPushEnable() {
        return pushEnable;
    }

    public void setPushEnable(boolean pushEnable) {
        this.pushEnable = pushEnable;
    }

    public boolean isPrEnable() {
        return prEnable;
    }

    public void setPrEnable(boolean prEnable) {
        this.prEnable = prEnable;
    }

    public Map<String, String> toEnv() {
        HashMap<String, String> env = new HashMap<>();
        env.put(GitToggleEnvs.FLOW_GIT_PUSH_ENABLED.name(), Boolean.toString(pushEnable));
        env.put(GitToggleEnvs.FLOW_GIT_TAG_ENABLED.name(), Boolean.toString(tagEnable));
        env.put(GitToggleEnvs.FLOW_GIT_PR_ENABLED.name(), Boolean.toString(prEnable));

        env.put(GitToggleEnvs.FLOW_GIT_PUSH_FILTER.name(), Jsonable.GSON_CONFIG.toJson(branchFilter));
        env.put(GitToggleEnvs.FLOW_GIT_TAG_FILTER.name(), Jsonable.GSON_CONFIG.toJson(tagFilter));
        return env;
    }
}
