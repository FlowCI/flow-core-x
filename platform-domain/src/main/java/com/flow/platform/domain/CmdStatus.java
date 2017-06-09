package com.flow.platform.domain;

/**
 * Status for TYPE.RUN_SHELL
 *
 * Created by gy@fir.im on 09/06/2017.
 * Copyright fir.im
 */
public enum CmdStatus {

    /**
     * Init status when cmd prepare send to agent
     * is_current_cmd = true
     */
    PENDING("PENDING"),

    /**
     * Cmd is running
     * is_current_cmd = true
     */
    RUNNING("RUNNING"), // current cmd

    /**
     * Cmd executed but not finish logging
     * is_current_cmd = true
     */
    EXECUTED("EXECUTED"), // current cmd

    /**
     * Log uploaded, cmd completely finished
     * is_current_cmd = false
     */
    LOGGED("LOGGED"),

    /**
     * Got exception when running
     * is_current_cmd = false
     */
    EXCEPTION("EXCEPTION"),

    /**
     * Killed by controller
     * is_current_cmd = false
     */
    KILLED("KILLED"),

    /**
     * Cannot execute since over agent limit
     * is_current_cmd = false
     */
    REJECTED("REJECTED"),

    /**
     * Cmd exec timeout which is found by scheduler task
     */
    TIMEOUT("TIMEOUT");

    private String name;

    CmdStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
