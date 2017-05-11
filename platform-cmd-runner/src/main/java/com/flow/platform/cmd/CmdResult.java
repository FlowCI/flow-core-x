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
    private Integer pid;

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

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
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
        return String.format("Cmd: pid=%s, exitValue=%s, duration=%s", pid, exitValue, duration);
    }
}
