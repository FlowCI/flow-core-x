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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command object to communicate between c/s
 * <p>
 *
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
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
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String logPath;

    /**
     * Retry time if cmd with cmd queue
     */
    @Getter
    @Setter
    private Integer retry = 0;

    /**
     * finish time
     */
    @Getter
    @Setter
    private ZonedDateTime finishedDate;

    /**
     * Created date
     */
    @Getter
    @Setter
    private ZonedDateTime createdDate;

    /**
     * Updated date
     */
    @Getter
    @Setter
    private ZonedDateTime updatedDate;

    /**
     * Cmd result
     */
    @Getter
    @Setter
    private CmdResult cmdResult;

    public Cmd(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
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

    public Boolean isCurrent() {
        return WORKING_STATUS.contains(status);
    }

    public Boolean isAgentCmd() {
        return AGENT_CMD_TYPE.contains(type);
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
        cmd.inputs.putAll(base.getInputs());
        cmd.workingDir = base.getWorkingDir();
        cmd.sessionId = base.getSessionId();
        cmd.outputEnvFilter.addAll(base.getOutputEnvFilter());
        cmd.webhook = base.getWebhook();
        cmd.extra = base.getExtra();
        return cmd;
    }
}
