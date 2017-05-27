package com.flow.platform.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class CmdResult implements Serializable {

    public static final Integer EXIT_VALUE_FOR_KILL = 143;
    public static final Integer EXIT_VALUE_FOR_REJECT = -100;

    /**
     * Only agent local, cannot parse to json
     */
    private transient Process process;

    /**
     * Process id
     */
    private Integer processId;

    /**
     * Linux exit status code, -100 means killed
     */
    private Integer exitValue;

    /**
     * Cmd running duration in second
     */
    private Long duration;

    /**
     * Cmd duration with logging in second
     */
    private Long totalDuration;

    /**
     * Cmd start time
     */
    private Date startTime;

    /**
     * Cmd executed time
     */
    private Date executedTime;

    /**
     * Cmd finish time with logging
     */
    private Date finishTime;

    /**
     * Exception while cmd running
     */
    private final List<Exception> exceptions = new ArrayList<>(5);

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public Integer getExitValue() {
        return exitValue;
    }

    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue;
    }

    public Long getDuration() {
        return duration;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getExecutedTime() {
        return executedTime;
    }

    public void setExecutedTime(Date executedTime) {
        this.executedTime = executedTime;
        this.duration = (this.executedTime.getTime() - this.startTime.getTime()) / 1000;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
        this.totalDuration = (this.finishTime.getTime() - this.startTime.getTime()) / 1000;
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
                '}';
    }
}
