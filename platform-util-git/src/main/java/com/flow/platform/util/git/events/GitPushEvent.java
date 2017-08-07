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

package com.flow.platform.util.git.events;

import com.flow.platform.util.git.Commit;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * @author yang
 */
public class GitPushEvent implements Serializable {

    /**
     * Git event type
     *  - Gitlab: object_kind
     */
    @SerializedName(value = "object_kind")
    private String type;

    /**
     * Commit SHA before event
     */
    @SerializedName(value = "before")
    private String before;

    /**
     * Commit SHA after event
     *  - Gitlab: after
     *  - Github: head
     */
    @SerializedName(value = "after", alternate = "head")
    private String after;

    /**
     * Branch ref info
     *  - Gitlab: ex: refs/heads/master
     */
    @SerializedName(value = "ref")
    private String ref;

    @SerializedName(value = "user_id")
    private String userId;

    @SerializedName(value = "user_name")
    private String username;

    @SerializedName(value = "total_commits_count", alternate = "distinct_size")
    private Integer numOfCommits;

    /**
     * Commit info
     */
    @SerializedName(value = "commits")
    private List<GitEventCommit> commits;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getNumOfCommits() {
        return numOfCommits;
    }

    public void setNumOfCommits(Integer numOfCommits) {
        this.numOfCommits = numOfCommits;
    }

    public List<GitEventCommit> getCommits() {
        return commits;
    }

    public void setCommits(List<GitEventCommit> commits) {
        this.commits = commits;
    }
}
