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
