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

/**
 * Status for TYPE.RUN_SHELL
 *
 * @author gy@fir.im
 */
public enum CmdStatus {

    /**
     * Init status when cmd prepare send to agent
     * is_current_cmd = true
     */
    PENDING("PENDING", 0),

    /**
     * Cmd is running, should agent reported
     * is_current_cmd = true
     */
    RUNNING("RUNNING", 1), // current cmd

    /**
     * Cmd executed but not finish logging, should agent reported
     * is_current_cmd = true
     */
    EXECUTED("EXECUTED", 2), // current cmd

    /**
     * Log uploaded, cmd completely finished, should agent reported
     * is_current_cmd = false
     */
    LOGGED("LOGGED", 3),

    /**
     * Got exception when running, should agent reported
     * is_current_cmd = false
     */
    EXCEPTION("EXCEPTION", 3),

    /**
     * Killed by controller, should agent reported
     * is_current_cmd = false
     */
    KILLED("KILLED", 3),

    /**
     * Cannot execute since over agent limit
     * is_current_cmd = false
     */
    REJECTED("REJECTED", 3),

    /**
     * Cmd exec timeout which is found by scheduler task
     */
    TIMEOUT_KILL("TIMEOUT_KILL", 4);

    private String name;

    public Integer getLevel() {
        return level;
    }

    private Integer level;

    CmdStatus(String name, Integer level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }
}
