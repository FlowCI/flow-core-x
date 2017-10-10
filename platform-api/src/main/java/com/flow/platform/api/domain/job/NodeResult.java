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

import static com.flow.platform.api.domain.job.NodeStatus.ENQUEUE;
import static com.flow.platform.api.domain.job.NodeStatus.FAILURE;
import static com.flow.platform.api.domain.job.NodeStatus.PENDING;
import static com.flow.platform.api.domain.job.NodeStatus.RUNNING;
import static com.flow.platform.api.domain.job.NodeStatus.STOPPED;
import static com.flow.platform.api.domain.job.NodeStatus.SUCCESS;
import static com.flow.platform.api.domain.job.NodeStatus.TIMEOUT;

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class NodeResult extends CreateUpdateObject {

    public final static EnumSet<NodeStatus> RUNNING_STATUS = EnumSet.of(PENDING, RUNNING, ENQUEUE);

    public final static EnumSet<NodeStatus> FINISH_STATUS = EnumSet.of(SUCCESS, TIMEOUT, FAILURE, STOPPED);

    public final static EnumSet<NodeStatus> SUCCESS_STATUS = EnumSet.of(SUCCESS);

    public final static EnumSet<NodeStatus> FAILURE_STATUS = EnumSet.of(TIMEOUT, FAILURE);

    public final static EnumSet<NodeStatus> STOP_STATUS = EnumSet.of(STOPPED);

    @Expose
    private NodeResultKey key;

    @Expose
    private Map<String, String> outputs = new LinkedHashMap<>();

    @Expose
    private Long duration = 0L;

    @Expose
    private Integer exitCode;

    @Expose
    private String logPath;

    @Expose
    private NodeStatus status = PENDING;

    @Expose
    private String cmdId;

    @Expose
    private NodeTag nodeTag;

    @Expose
    private ZonedDateTime startTime;

    @Expose
    private ZonedDateTime finishTime;

    @Expose
    private String name; // node name

    @Expose
    private String failureMessage;

    @Expose
    private Integer order;

    @Expose
    private String createdBy;

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

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public boolean isRunning() {
        return RUNNING_STATUS.contains(status);
    }

    public boolean isSuccess() {
        return SUCCESS_STATUS.contains(status);
    }

    public boolean isFailure() {
        return FAILURE_STATUS.contains(status);
    }

    public boolean isStop() {
        return STOP_STATUS.contains(status);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

        NodeResult that = (NodeResult) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    @Override
    public String toString() {
        return "NodeResult{" +
            "key=" + key +
            ", exitCode=" + exitCode +
            ", status=" + status +
            "} " + super.toString();
    }
}
