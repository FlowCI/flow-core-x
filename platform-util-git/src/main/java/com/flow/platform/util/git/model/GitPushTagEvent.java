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

package com.flow.platform.util.git.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Event object for git push and create tag event
 *
 * @author yang
 */
public class GitPushTagEvent extends GitEvent {

    /**
     * To generate compare id as {12}...{12}
     *
     * @param event source event must contain before and after property
     */
    public static String buildCompareId(GitPushTagEvent event) {
        if (event.getType() == GitEventType.PUSH) {
            String beforeShortcut = event.getBefore().substring(0, 12);
            String afterShortcut = event.getAfter().substring(0, 12);
            return beforeShortcut + "..." + afterShortcut;
        }

        if (event.getType() == GitEventType.TAG) {
            String afterShortcut = event.getAfter().substring(0, 12);
            int tagVersionIndex = event.getRef().lastIndexOf("/");
            String tag = event.getRef().substring(tagVersionIndex + 1);
            return afterShortcut + "..." + tag;
        }

        return "";
    }

    /**
     * Commit SHA before event
     */
    @SerializedName(value = "before")
    private String before;

    /**
     * Commit SHA after event
     * - Gitlab: after
     * - Github: head
     */
    @SerializedName(value = "after", alternate = "head")
    private String after;

    /**
     * The url for head commit
     */
    private String headCommitUrl;

    /**
     * Branch ref info
     * - Gitlab: ex: refs/heads/master
     */
    @SerializedName(value = "ref")
    private String ref;

    @SerializedName(value = "base_ref")
    private String baseRef;

    /**
     * User id who send this event
     */
    @SerializedName(value = "user_id")
    private String userId;

    /**
     * User name who end this event
     */
    @SerializedName(value = "user_name")
    private String username;

    private String message;

    @SerializedName(value = "compare")
    private String compareUrl;

    /**
     * Compare id with 123...123 format
     */
    private String compareId;

    /**
     * Commit info
     */
    @SerializedName(value = "commits")
    private List<GitEventCommit> commits;

    public GitPushTagEvent(GitSource gitSource, GitEventType type) {
        super(gitSource, type);
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

    public String getHeadCommitUrl() {
        return headCommitUrl;
    }

    public void setHeadCommitUrl(String headCommitUrl) {
        this.headCommitUrl = headCommitUrl;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public void setBaseRef(String baseRef) {
        this.baseRef = baseRef;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCompareUrl() {
        return compareUrl;
    }

    public void setCompareUrl(String compareUrl) {
        this.compareUrl = compareUrl;
    }

    public String getCompareId() {
        return compareId;
    }

    public void setCompareId(String compareId) {
        this.compareId = compareId;
    }

    public List<GitEventCommit> getCommits() {
        return commits;
    }

    public void setCommits(List<GitEventCommit> commits) {
        this.commits = commits;
    }

    @Override
    public String getTitle() {
        return message;
    }
}
