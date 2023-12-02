/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.agent.domain.ShellOut;
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;


/**
 * ExecutedCmd == Step node with executed status and data
 *
 * @author yang
 */
@Data
@NoArgsConstructor
@Document(collection = "step")
@Accessors(chain = true)
@CompoundIndex(
        name = "index_job_id_and_node_path",
        def = "{'jobId': 1, 'nodePath': 1}",
        unique = true
)
public class Step implements Executed {

    public enum Type {
        STEP,

        STAGE, // step with children

        FLOW,

        PARALLEL,
    }

    public static final String MessageSkippedOnCondition = "Skipped due to condition";

    @Id
    private String id;

    private String flowId;

    private Long buildNumber;

    @Indexed(name = "index_job_id")
    private String jobId;

    private String agentId;

    private String nodePath;

    private Type type;

    private boolean allowFailure;

    private boolean post;

    private String plugin;

    private List<DockerOption> dockers;

    // parent node path
    private String parent;

    // next step node path
    private List<String> next;

    /**
     * Process id
     */
    private Integer processId;

    /**
     * Container id if executed from container
     */
    private String containerId;

    /**
     * Cmd execution status
     */
    private Status status = Status.PENDING;

    /**
     * Linux shell exit code
     */
    private Integer code;

    /**
     * Cmd output
     */
    private Vars<String> output = new StringVars();

    /**
     * Cmd start at timestamp
     */
    private Date startAt;

    /**
     * Cmd finish at timestamp
     */
    private Date finishAt;

    /**
     * Error message
     */
    private String error;

    /**
     * Num of line of the log
     */
    private Long logSize = -1L;

    @JsonIgnore
    public boolean hasAgent() {
        return StringHelper.hasValue(agentId);
    }

    @JsonIgnore
    public boolean hasParent() {
        return StringHelper.hasValue(parent);
    }

    @JsonIgnore
    public boolean isRoot() {
        return !hasParent();
    }

    @JsonIgnore
    public boolean isStepType() {
        return this.type == Type.STEP || this.type == Type.STAGE;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SuccessStatus.contains(status) ||
                (FailureStatus.contains(status) && isAllowFailure());
    }

    @JsonIgnore
    public boolean isOngoing() {
        return OngoingStatus.contains(status);
    }

    @JsonIgnore
    public boolean isKilling() {
        return status == Status.KILLING;
    }

    @JsonIgnore
    public boolean isFinished() {
        return FinishStatus.contains(status);
    }

    public void setFrom(ShellOut out) {
        this.processId = out.getProcessId();
        this.containerId = out.getContainerId();
        this.status = out.getStatus();
        this.code = out.getCode();
        this.output = out.getOutput();
        this.startAt = out.getStartAt();
        this.finishAt = out.getFinishAt();
        this.error = out.getError();
        this.logSize = out.getLogSize();
    }
}
