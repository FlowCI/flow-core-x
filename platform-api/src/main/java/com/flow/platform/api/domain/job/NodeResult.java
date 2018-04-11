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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(of = {"key"}, callSuper = false)
@ToString(of = {"key", "exitCode", "status"})
public class NodeResult extends CreateUpdateObject {

    public final static EnumSet<NodeStatus> RUNNING_STATUS = EnumSet.of(PENDING, RUNNING, ENQUEUE);

    public final static EnumSet<NodeStatus> FINISH_STATUS = EnumSet.of(SUCCESS, TIMEOUT, FAILURE, STOPPED);

    public final static EnumSet<NodeStatus> SUCCESS_STATUS = EnumSet.of(SUCCESS);

    public final static EnumSet<NodeStatus> FAILURE_STATUS = EnumSet.of(TIMEOUT, FAILURE);

    public final static EnumSet<NodeStatus> STOP_STATUS = EnumSet.of(STOPPED);

    @Expose
    @Getter
    @Setter
    private NodeResultKey key;

    @Expose
    @Getter
    @Setter
    private Map<String, String> outputs = new LinkedHashMap<>();

    @Expose
    @Getter
    @Setter
    private Long duration = 0L;

    @Expose
    @Getter
    @Setter
    private Integer exitCode;

    @Expose
    @Getter
    @Setter
    private String logPath;

    @Expose
    @Getter
    @Setter
    private NodeStatus status = PENDING;

    @Expose
    @Getter
    @Setter
    private String cmdId;

    @Expose
    @Getter
    @Setter
    private NodeTag nodeTag;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime startTime;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime finishTime;

    @Expose
    @Getter
    @Setter
    private String name; // node name

    @Expose
    @Getter
    @Setter
    private String failureMessage;

    @Expose
    @Getter
    @Setter
    private Integer order;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    public NodeResult() {
    }

    public NodeResult(BigInteger jobId, String path) {
        this(new NodeResultKey(jobId, path));
    }

    public NodeResult(NodeResultKey key) {
        this.key = key;
    }

    public String getPath() {
        return this.key.getPath();
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
}
