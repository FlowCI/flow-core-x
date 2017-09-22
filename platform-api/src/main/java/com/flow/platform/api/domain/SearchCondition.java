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

package com.flow.platform.api.domain;

import com.flow.platform.util.git.model.GitEventType;

/**
 * @author yh@firim
 */
public class SearchCondition {

    private String keyword;

    private String branch;

    private GitEventType gitEventType;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public GitEventType getGitEventType() {
        return gitEventType;
    }

    public void setGitEventType(GitEventType gitEventType) {
        this.gitEventType = gitEventType;
    }

    @Override
    public String toString() {
        return "SearchCondition{" +
            "keyword='" + keyword + '\'' +
            ", branch='" + branch + '\'' +
            ", gitEventType=" + gitEventType +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SearchCondition that = (SearchCondition) o;

        if (keyword != null ? !keyword.equals(that.keyword) : that.keyword != null) {
            return false;
        }
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        return gitEventType == that.gitEventType;
    }

    @Override
    public int hashCode() {
        int result = keyword != null ? keyword.hashCode() : 0;
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (gitEventType != null ? gitEventType.hashCode() : 0);
        return result;
    }
}
