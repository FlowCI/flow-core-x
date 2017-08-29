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

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeResult extends Jsonable {

    private NodeResultKey key;

    @Expose
    private Map<String, String> outputs = new HashMap<>();

    @Expose
    private Long duration = 0L;

    @Expose
    private Integer exitCode;

    @Expose
    private List<String> logPaths = new ArrayList<>();

    @Expose
    private NodeStatus status = NodeStatus.PENDING;

    @Expose
    private String cmdId;

    @Expose
    private NodeTag nodeTag;

    @Expose
    private ZonedDateTime startTime;

    @Expose
    private ZonedDateTime finishTime;

    private String name; // node name

    private int order;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    public NodeResult() {
    }

    public NodeResult(BigInteger jobId, String path) {
        this(new NodeResultKey(jobId, path));
    }

    public NodeResult(NodeResultKey key) {
        this.key = key;
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

    public NodeResultKey getKey() {
        return key;
    }

    public void setKey(NodeResultKey key) {
        this.key = key;
    }

    public BigInteger getJobId() {
        return this.key.getJobId();
    }

    public String getPath() {
        return this.key.getPath();
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
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

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }
}
