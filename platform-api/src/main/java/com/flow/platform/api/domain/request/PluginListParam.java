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

import com.flow.platform.plugin.domain.PluginStatus;
import java.util.Set;

/**
 * @author yang
 */
public class PluginListParam {

    private Set<PluginStatus> status;

    private String keyword;

    private Set<String> labels;

    public PluginListParam() {
    }

    public PluginListParam(Set<PluginStatus> status, String keyword, Set<String> labels) {
        this.status = status;
        this.keyword = keyword;
        this.labels = labels;
    }

    public Set<PluginStatus> getStatus() {
        return status;
    }

    public void setStatus(Set<PluginStatus> status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }
}
