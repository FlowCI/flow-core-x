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

import com.flow.platform.api.domain.adaptor.EnvAdaptor;
import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder.In;

public class Job extends EnvObject {

    @Expose
    private BigInteger id;

    @Expose
    private Long duration = 0l;

    @Expose
    private String nodePath;

    private Integer exitCode;

    private String sessionId;

    private String cmdId;

    private NodeResult nodeResult;

    @Expose
    private NodeStatus status;

    @Expose
    @JsonAdapter(EnvAdaptor.class)
    private Map<String, String> outputs = new HashMap<>();

    @Expose
    private String nodeName;

    @Expose
    private String branch;

    @Expose
    private ZonedDateTime startedAt;

    @Expose
    private ZonedDateTime finishedAt;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    @Expose
    private Integer number;

    public Job(BigInteger id, Integer number, String nodeName, String nodePath, String sessionId,
        ZonedDateTime startedAt, ZonedDateTime finishedAt, NodeStatus status, Integer exitCode,
        Map<String, String> outputs, Long duration, String cmdId, ZonedDateTime createdAt, ZonedDateTime updatedAt) {

        this.id = id;
        this.number = number;
        this.nodeName = nodeName;
        this.nodePath = nodePath;
        this.sessionId = sessionId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.status = status;
        this.exitCode = exitCode;
        this.outputs = outputs;
        this.duration = duration;
        this.cmdId = cmdId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Job(BigInteger id) {
        this.id = id;
    }

    public NodeResult getNodeResult() {
        return nodeResult;
    }

    public void setNodeResult(NodeResult nodeResult) {
        this.nodeResult = nodeResult;
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public ZonedDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(ZonedDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
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
            ", duration=" + duration +
            ", exitCode=" + exitCode +
            ", nodePath='" + nodePath + '\'' +
            ", sessionId='" + sessionId + '\'' +
            ", cmdId='" + cmdId + '\'' +
            ", status=" + status +
            '}';
    }
}
