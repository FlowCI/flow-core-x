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

package com.flow.platform.cc.domain;

import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import java.io.Serializable;

/**
 * Queue item for report cmd status
 *
 * @author yang
 */
public class CmdStatusItem implements Serializable {

    private String cmdId;

    private CmdStatus status;

    private CmdResult cmdResult;

    private boolean updateAgentStatus;

    private boolean callWebhook;

    public CmdStatusItem(String cmdId,
                         CmdStatus status,
                         CmdResult cmdResult,
                         boolean updateAgentStatus,
                         boolean callWebhook) {
        this.cmdId = cmdId;
        this.status = status;
        this.cmdResult = cmdResult;
        this.updateAgentStatus = updateAgentStatus;
        this.callWebhook = callWebhook;
    }

    public String getCmdId() {
        return cmdId;
    }

    public CmdStatus getStatus() {
        return status;
    }

    public CmdResult getCmdResult() {
        return cmdResult;
    }

    public boolean isUpdateAgentStatus() {
        return updateAgentStatus;
    }

    public boolean isCallWebhook() {
        return callWebhook;
    }

    @Override
    public String toString() {
        return "CmdStatusItem{" +
            "cmdId='" + cmdId + '\'' +
            ", status=" + status +
            ", cmdResult=" + cmdResult +
            ", updateAgentStatus=" + updateAgentStatus +
            ", callWebhook=" + callWebhook +
            '}';
    }
}
