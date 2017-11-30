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
import java.util.List;
import java.util.Map;

/**
 * Only include basic properties of command
 * <p>
 *
 * @author gy@fir.im
 */
public abstract class CmdBase extends Webhookable {

    /**
     * Destination of command
     * Agent name can be null, that means platform select a instance to run cmd
     */
    protected AgentPath agentPath;

    /**
     * Command type (Required)
     */
    protected CmdType type;

    /**
     * record current status
     */
    protected CmdStatus status = CmdStatus.PENDING;

    /**
     * Command content (Required when type = RUN_SHELL)
     */
    protected String cmd;

    /**
     * Cmd timeout in seconds
     */
    protected Integer timeout;

    /**
     * Platform will reserve a machine for session
     */
    protected String sessionId;

    /**
     * Input parameter, deal with export XX=XX before cmd execute
     * Add input: getInputs().add(key, value)
     */
    protected Map<String, String> inputs = new HashMap<>();

    /**
     * Cmd working dir, default is user.home
     */
    protected String workingDir;

    /**
     * Filter for env input to CmdResult.output map
     */
    protected List<String> outputEnvFilter;

    /**
     * Extra info used for webhook callback
     */
    protected String extra;

    public CmdBase() {
    }

    public CmdBase(String zone, String agent, CmdType type, String cmd) {
        this(new AgentPath(zone, agent), type, cmd);
    }

    public CmdBase(AgentPath agentPath, CmdType type, String cmd) {
        this.agentPath = agentPath;
        this.type = type;
        this.cmd = cmd;
    }

    public AgentPath getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(AgentPath agentPath) {
        this.agentPath = agentPath;
    }

    public String getZoneName() {
        if (agentPath == null) {
            return null;
        }
        return agentPath.getZone();
    }

    public String getAgentName() {
        if (agentPath == null) {
            return null;
        }
        return agentPath.getName();
    }

    public CmdType getType() {
        return type;
    }

    public void setType(CmdType type) {
        this.type = type;
    }

    public void setStatus(CmdStatus status) {
        this.status = status;
    }

    public CmdStatus getStatus() {
        return status;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean hasSession() {
        if (type == CmdType.CREATE_SESSION) {
            return false;
        }
        return sessionId != null;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public List<String> getOutputEnvFilter() {
        return outputEnvFilter;
    }

    public void setOutputEnvFilter(List<String> outputEnvFilter) {
        this.outputEnvFilter = outputEnvFilter;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CmdBase cmdBase = (CmdBase) o;

        return agentPath.equals(cmdBase.agentPath);
    }

    @Override
    public int hashCode() {
        return agentPath.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            " zone=" + (agentPath == null ? "null" : agentPath.getZone()) +
            ", agent=" + (agentPath == null ? "null" : agentPath.getName()) +
            ", status=" + status +
            ", type=" + type +
            '}';
    }
}
