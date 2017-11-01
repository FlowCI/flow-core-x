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

import java.util.List;

/**
 * @author lhl
 */
public class TriggerParam {

    private List<String> branchFilter;

    private List<String> tagFilter;

    private boolean tagEnabled;

    private boolean pushEnabled;

    private boolean prEnabled;

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

    public boolean isTagEnabled() {
        return tagEnabled;
    }

    public void setTagEnabled(boolean tagEnabled) {
        this.tagEnabled = tagEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isPrEnabled() {
        return prEnabled;
    }

    public void setPrEnabled(boolean prEnabled) {
        this.prEnabled = prEnabled;
    }
}
