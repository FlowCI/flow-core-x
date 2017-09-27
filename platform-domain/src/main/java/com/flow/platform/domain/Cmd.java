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

import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Command object to communicate between c/s
 * <p>
 *
 * @author gy@fir.im
 */
public class Cmd extends CmdBase {

    /**
     * Working status set
     */
    public static final Set<CmdStatus> WORKING_STATUS =
        Sets.newHashSet(CmdStatus.PENDING, CmdStatus.SENT, CmdStatus.RUNNING, CmdStatus.EXECUTED);

    /**
     * Finish status set
     */
    public static final Set<CmdStatus> FINISH_STATUS =
        Sets.newHashSet(CmdStatus.LOGGED, CmdStatus.EXCEPTION, CmdStatus.KILLED, CmdStatus.REJECTED,
            CmdStatus.TIMEOUT_KILL, CmdStatus.STOPPED);

    /**
     * The cmd type should handle in agent
     */
    public static final Set<CmdType> AGENT_CMD_TYPE =
        Sets.newHashSet(CmdType.RUN_SHELL, CmdType.SHUTDOWN, CmdType.KILL, CmdType.STOP);

    /**
     * Server generated command id
     */
    private String id;

    private String logPath;

    /**
     * Retry time if cmd with cmd queue
     */
    private Integer retry = 0;

    /**
     * finish time
     */
    private ZonedDateTime finishedDate;

    /**
     * Created date
     */
    private ZonedDateTime createdDate;

    /**
     * Updated date
     */
    private ZonedDateTime updatedDate;

    /**
     * Cmd result
     */
    private CmdResult cmdResult;

    public Cmd() {
    }

    public Cmd(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(ZonedDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public CmdResult getCmdResult() {
        return cmdResult;
    }

    public void setCmdResult(CmdResult cmdResult) {
        this.cmdResult = cmdResult;
    }

    /**
     * only level gt current level
     *
     * @param status target status
     * @return true if status updated
     */
    public boolean addStatus(CmdStatus status) {
        if (this.status == null) {
            this.status = status;
            return true;
        }

        if (this.status.getLevel() < status.getLevel()) {
            this.status = status;
            return true;
        }

        return false;
    }

    public boolean isCmdTimeout() {
        if (type != CmdType.RUN_SHELL) {
            return false;
        }

        // not timeout since cmd is executed
        if (!isCurrent()) {
            return false;
        }

        ZonedDateTime createdAt = getCreatedDate();
        final long runningInSeconds = ChronoUnit.SECONDS.between(createdAt, ZonedDateTime.now());
        return runningInSeconds >= getTimeout();
    }

    public Boolean isCurrent() {
        return WORKING_STATUS.contains(status);
    }

    public Boolean isAgentCmd() {
        return AGENT_CMD_TYPE.contains(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Cmd cmd = (Cmd) o;

        return id != null ? id.equals(cmd.id) : cmd.id == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cmd{" +
            "id='" + id + '\'' +
            ", retry='" + retry + '\'' +
            ", info=" + super.toString() +
            '}';
    }

    /**
     * Convert CmdBase to Cmd
     */
    public static Cmd convert(CmdBase base) {
        Cmd cmd = new Cmd();
        cmd.agentPath = base.getAgentPath();
        cmd.status = base.getStatus();
        cmd.type = base.getType();
        cmd.cmd = base.getCmd();
        cmd.timeout = base.getTimeout();
        cmd.inputs = base.getInputs();
        cmd.workingDir = base.getWorkingDir();
        cmd.sessionId = base.getSessionId();
        cmd.outputEnvFilter = base.getOutputEnvFilter();
        cmd.webhook = base.getWebhook();
        cmd.extra = base.getExtra();
        return cmd;
    }
}
