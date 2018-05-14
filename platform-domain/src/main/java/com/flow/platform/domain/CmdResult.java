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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"cmdId"}, callSuper = false)
public class CmdResult extends Jsonable {

    public static final Integer EXIT_VALUE_FOR_KILL = 143; // auto set while kill process

    public static final Integer EXIT_VALUE_FOR_REJECT = -100;

    public static final Integer EXIT_VALUE_FOR_TIMEOUT = -200;

    public static final CmdResult EMPTY = new CmdResult();

    /**
     * Only agent local, cannot parse to json
     */
    @Setter
    @Getter
    private transient Process process;

    /**
     * Related cmd id
     */
    @Setter
    @Getter
    private String cmdId;

    /**
     * Process id
     */
    @Setter
    @Getter
    private Integer processId;

    /**
     * Linux exit status code, -100 means killed
     */
    @Setter
    @Getter
    private Integer exitValue;

    /**
     * Cmd running duration in second
     */
    @Setter
    @Getter
    private Long duration;

    /**
     * Cmd duration with logging in second
     */
    @Setter
    @Getter
    private Long totalDuration;

    /**
     * Cmd start time
     */
    @Setter
    @Getter
    private ZonedDateTime startTime;

    /**
     * Cmd executed time
     */
    @Getter
    private ZonedDateTime executedTime;

    /**
     * Cmd finish time with logging
     */
    @Getter
    private ZonedDateTime finishTime;

    /**
     * Env for output
     */
    @Setter
    @Getter
    private Map<String, String> output = new HashMap<>(5);

    /**
     * Exception while cmd running
     */
    @Setter
    @Getter
    private List<Throwable> exceptions = new ArrayList<>(5);

    public CmdResult(Integer exitValue) {
        this.exitValue = exitValue;
    }

    public void setExecutedTime(ZonedDateTime executedTime) {
        if (executedTime != null) {
            this.executedTime = executedTime;
            if (startTime != null) {
                this.duration = ChronoUnit.SECONDS.between(this.startTime, this.executedTime);
                this.totalDuration = this.duration;
            }
        }
    }

    public void setFinishTime(ZonedDateTime finishTime) {
        if (finishTime != null) {
            this.finishTime = finishTime;
            if (this.startTime != null) {
                this.totalDuration = ChronoUnit.SECONDS.between(this.startTime, this.finishTime);
            }
        }
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {
        return "CmdResult{" +
            "processId=" + processId +
            ", exitValue=" + exitValue +
            ", duration=" + duration +
            ", totalDuration=" + totalDuration +
            ", startTime=" + startTime +
            ", executedTime=" + executedTime +
            ", finishTime=" + finishTime +
            ", outputSize=" + output.size() +
            '}';
    }
}
