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

package com.flow.platform.api.domain.job;

import com.flow.platform.api.domain.EnvObject;
import com.flow.platform.util.git.model.GitEventType;
import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * @author yh@firim
 */

public class Job extends EnvObject {

    public final static EnumSet<JobStatus> FINISH_STATUS = EnumSet
        .of(JobStatus.FAILURE, JobStatus.STOPPED, JobStatus.SUCCESS, JobStatus.TIMEOUT);

    public final static EnumSet<JobStatus> RUNNING_STATUS = EnumSet
        .of(JobStatus.RUNNING);

    public final static EnumSet<JobStatus> FAILURE_STATUS = EnumSet
        .of(JobStatus.FAILURE, JobStatus.STOPPED, JobStatus.TIMEOUT);

    public final static EnumSet<JobStatus> SUCCESS_STATUS = EnumSet
        .of(JobStatus.SUCCESS);

    private BigInteger id;

    @Expose
    private String nodePath;

    @Expose
    private Integer number;

    private String sessionId;

    @Expose
    private String nodeName;

    @Expose
    private GitEventType category = GitEventType.MANUAL;

    @Expose
    private JobStatus status = JobStatus.CREATED;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    private String logPath;

    @Expose
    private String failureMessage;

    /**
     * The root node result for job detail
     */
    @Expose
    @SerializedName("result")
    private NodeResult rootResult;

    /**
     * The node result list expect root result
     */
    @Expose
    private List<NodeResult> childrenResult;

    @Expose
    private String createdBy;

    public Job(BigInteger id) {
        this.id = id;
    }

    public Job() {
    }

    public NodeResult getRootResult() {
        return rootResult;
    }

    public void setRootResult(NodeResult rootResult) {
        this.rootResult = rootResult;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<NodeResult> getChildrenResult() {
        return childrenResult;
    }

    public void setChildrenResult(List<NodeResult> childrenResult) {
        this.childrenResult = childrenResult;
    }

    public GitEventType getCategory() {
        return category;
    }

    public void setCategory(GitEventType category) {
        this.category = category;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Job job = (Job) o;

        return id.equals(job.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Job{" +
            "id='" + id + '\'' +
            ", nodePath='" + nodePath + '\'' +
            ", sessionId='" + sessionId + '\'' +
            '}';
    }
}