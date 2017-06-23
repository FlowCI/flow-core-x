package com.flow.platform.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class CmdResult implements Serializable {

    public static final Integer EXIT_VALUE_FOR_KILL = 143; // auto set while kill process
    public static final Integer EXIT_VALUE_FOR_REJECT = -100;
    public static final Integer EXIT_VALUE_FOR_TIMEOUT = -200;

    /**
     * Only agent local, cannot parse to json
     */
    private transient Process process;

    /**
     * Related cmd id
     */
    private String cmdId;

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
    private ZonedDateTime startTime;

    /**
     * Cmd executed time
     */
    private ZonedDateTime executedTime;

    /**
     * Cmd finish time with logging
     */
    private ZonedDateTime finishTime;

    /**
     * Env for output
     */
    private Map<String, String> output = new HashMap<>(5);

    /**
     * Exception while cmd running
     */
    private List<Throwable> exceptions = new ArrayList<>(5);

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

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

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getExecutedTime() {
        return executedTime;
    }

    public void setExecutedTime(ZonedDateTime executedTime) {
        if(executedTime != null){
            this.executedTime = executedTime;
            this.duration = ChronoUnit.SECONDS.between(this.startTime, this.executedTime);
        }
    }

    public ZonedDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(ZonedDateTime finishTime) {
        if(finishTime != null){
            this.finishTime = finishTime;
            if(this.startTime != null){
                this.totalDuration = ChronoUnit.SECONDS.between(this.startTime, this.finishTime);
            }
        }
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Map<String, String> getOutput() {
        return output;
    }

    public void setOutput(Map<String, String> output) {
        this.output = output;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<Throwable> exceptions) {
        this.exceptions = exceptions;
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
