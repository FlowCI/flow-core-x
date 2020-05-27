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
import com.flowci.domain.CmdBase;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;


/**
 * ExecutedCmd == Step node with executed status and data
 *
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "executed_cmd")
@CompoundIndex(
        name = "index_job_id_and_node_path",
        def = "{'jobId': 1, 'nodePath': 1}",
        unique = true
)
public class ExecutedCmd extends CmdBase implements Executed {

    /**
     * Process id
     */
    private Integer processId;

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

    public ExecutedCmd(String flowId, String jobId, String nodePath, boolean allowFailure) {
        setFlowId(flowId);
        setJobId(jobId);
        setNodePath(nodePath);
        setAllowFailure(allowFailure);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SuccessStatus.contains(status) || isAllowFailure();
    }

    @JsonIgnore
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @JsonIgnore
    public boolean isPending() {
        return status == Status.PENDING;
    }
}
