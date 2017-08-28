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

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;

/**
 * @author yh@firim
 */
public enum NodeStatus {

    PENDING("PENDING", 0),

    //enter queue
    ENQUEUE("ENQUEUE", 1),

    RUNNING("RUNNING", 2),

    SUCCESS("SUCCESS", 3),

    STOPPED("STOPPED", 3),

    FAILURE("FAILURE", 3),

    TIMEOUT("TIMEOUT", 3);

    private String name;

    public Integer getLevel() {
        return level;
    }

    private Integer level;

    NodeStatus(String name, Integer level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    /**
     * Transfer cmd to node status
     */
    public static NodeStatus transfer(Cmd cmd) {
        if (cmd == null || cmd.getStatus() == null) {
            throw new IllegalParameterException("Cannot transfer null cmd or null cmd status to node status");
        }

        CmdStatus status = cmd.getStatus();

        switch (status) {
            case SENT:
            case PENDING:
                return NodeStatus.PENDING;

            case RUNNING:
            case EXECUTED:
                return NodeStatus.RUNNING;

            case LOGGED:
                CmdResult cmdResult = cmd.getCmdResult();
                if (cmdResult != null) {
                    Integer exitCode = cmdResult.getExitValue();
                    if (exitCode != null && exitCode == 0) {
                        return NodeStatus.SUCCESS;
                    }
                }

                return NodeStatus.FAILURE;

            case KILLED:
            case EXCEPTION:
            case REJECTED:
                return NodeStatus.FAILURE;

            case STOPPED:
                return NodeStatus.STOPPED;

            case TIMEOUT_KILL:
                return NodeStatus.TIMEOUT;
        }

        throw new IllegalStatusException("Unsupported cmd status " + status + " transfer to node status");
    }
}
