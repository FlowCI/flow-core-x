package com.flow.platform.cmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gy@fir.im on 12/05/2017.
 *
 * @copyright fir.im
 */
public class CmdResult {

    /**
     * Process id
     */
    private Process process;

    /**
     * Linux exit status code
     */
    private Integer exitValue;

    /**
     * Cmd running duration in second
     */
    private Long duration;

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

    public Integer getExitValue() {
        return exitValue;
    }

    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public String toString() {
        return String.format("Cmd: process=%s, exitValue=%s, duration=%s", process, exitValue, duration);
    }
}
