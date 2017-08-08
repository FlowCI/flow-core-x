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

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.*;

public class NodeResult {

    private NodeResultKey nodeResultKey;

    private Map<String, String> outputs = new HashMap<>();

    private Long duration;

    private Integer exitCode;

    private List<String> logPaths = new ArrayList<>();

    private NodeStatus status = NodeStatus.PENDING;

    private String cmdId;

    private NodeTag nodeTag;

    private ZonedDateTime startTime;

    private ZonedDateTime finishTime;

    private String name;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    public NodeResult() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public NodeResult(BigInteger jobId, String path) {
        this(new NodeResultKey(jobId, path));
    }

    public NodeResult(NodeResultKey nodeResultKey) {
        this.nodeResultKey = nodeResultKey;
    }

    public NodeResultKey getNodeResultKey() {
        return nodeResultKey;
    }

    public void setNodeResultKey(NodeResultKey nodeResultKey) {
        this.nodeResultKey = nodeResultKey;
    }

    public BigInteger getJobId() {
        return this.nodeResultKey.getJobId();
    }

    public String getPath() {
        return this.nodeResultKey.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeResult that = (NodeResult) o;

        return nodeResultKey.equals(that.nodeResultKey);
    }

    @Override
    public int hashCode() {
        return  this.getNodeResultKey().hashCode();
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public List<String> getLogPaths() {
        return logPaths;
    }

    public void setLogPaths(List<String> logPaths) {
        this.logPaths = logPaths;
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

    public NodeTag getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(NodeTag nodeTag) {
        this.nodeTag = nodeTag;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(ZonedDateTime finishTime) {
        this.finishTime = finishTime;
    }
}
