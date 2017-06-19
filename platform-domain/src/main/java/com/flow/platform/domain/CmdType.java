package com.flow.platform.domain;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 09/06/2017.
 * Copyright fir.im
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
