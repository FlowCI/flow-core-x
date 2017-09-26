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

/**
 * Pull Request with Closed event (Merge Request)
 *
 * @author yang
 */
public class GitPullRequestEvent extends GitEvent {

    public enum State {
        OPEN,

        CLOSE
    }

    private String title;

    private Integer requestId;

    private GitPullRequestInfo source;

    private GitPullRequestInfo target;

    private String action;

    private String description;

    private State state;

    /**
     * Html url for pr
     */
    private String url;

    /**
     * Username who submit this pr
     */
    private String submitter;

    /**
     * Username who merge this pr
     */
    private String mergedBy;

    public GitPullRequestEvent(GitSource gitSource, GitEventType type) {
        super(gitSource, type);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public GitPullRequestInfo getSource() {
        return source;
    }

    public void setSource(GitPullRequestInfo source) {
        this.source = source;
    }

    public GitPullRequestInfo getTarget() {
        return target;
    }

    public void setTarget(GitPullRequestInfo target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getMergedBy() {
        return mergedBy;
    }

    public void setMergedBy(String mergedBy) {
        this.mergedBy = mergedBy;
    }
}
