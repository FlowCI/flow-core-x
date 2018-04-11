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

package com.flow.platform.domain;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Only include basic properties of command
 * <p>
 *
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"agentPath"}, callSuper = false)
public abstract class CmdBase extends Webhookable {

    /**
     * Destination of command
     * Agent name can be null, that means platform select a instance to run cmd
     */
    @Setter
    @Getter
    protected AgentPath agentPath;

    /**
     * Command type (Required)
     */
    @Setter
    @Getter
    protected CmdType type;

    /**
     * record current status
     */
    @Setter
    @Getter
    protected CmdStatus status = CmdStatus.PENDING;

    /**
     * Command content (Required when type = RUN_SHELL)
     */
    @Setter
    @Getter
    protected String cmd;

    /**
     * Cmd timeout in seconds
     */
    @Setter
    @Getter
    protected Integer timeout;

    /**
     * Platform will reserve a machine for session
     */
    @Setter
    @Getter
    protected String sessionId;

    /**
     * Input parameter, deal with export XX=XX before cmd execute
     * Add input: getInputs().add(key, value)
     */
    @Setter
    @Getter
    protected Map<String, String> inputs = new HashMap<>();

    /**
     * Cmd working dir, default is user.home
     */
    @Setter
    @Getter
    protected String workingDir;

    /**
     * Filter for env input to CmdResult.output map
     */
    @Setter
    @Getter
    protected List<String> outputEnvFilter = new LinkedList<>();

    /**
     * Extra info used for webhook callback
     */
    @Setter
    @Getter
    protected String extra;

    public CmdBase(String zone, String agent, CmdType type, String cmd) {
        this(new AgentPath(zone, agent), type, cmd);
    }

    public CmdBase(AgentPath agentPath, CmdType type, String cmd) {
        this.agentPath = agentPath;
        this.type = type;
        this.cmd = cmd;
    }

    public String getZoneName() {
        return Objects.isNull(agentPath) ? null : agentPath.getZone();
    }

    public String getAgentName() {
        return Objects.isNull(agentPath) ? null : agentPath.getName();
    }

    public boolean hasSession() {
        return type != CmdType.CREATE_SESSION && sessionId != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            " zone=" + getZoneName() +
            ", agent=" + getAgentName() +
            ", status=" + status +
            ", type=" + type +
            '}';
    }
}
