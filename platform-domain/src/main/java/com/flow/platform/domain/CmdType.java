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

import java.io.Serializable;

/**
 * @author gy@fir.im
 */
public enum CmdType implements Serializable {

    /**
     * Run a shell script in agent
     */
    RUN_SHELL("RUN_SHELL"),

    /**
     * Find an agent and create session for it
     */
    CREATE_SESSION("CREATE_SESSION"),

    /**
     * Release agent for session
     */
    DELETE_SESSION("DELETE_SESSION"),

    /**
     * KILL current running processes
     */
    KILL("KILL"),

    /**
     * Stop agent, and delete session
     */
    STOP("STOP"),

    /**
     * run other command, do not affect step command running
     */
    OTHER("OTHER"),

    /**
     * Stop agent, delete session and shutdown machine
     */
    SHUTDOWN("SHUTDOWN");

    private String name;

    CmdType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
