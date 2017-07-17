/*
 * *
 *  * Created by yh@fir.im
 *  * Copyright fir.im
 *
 */

package com.flow.platform.api.domain;

import java.util.*;

public abstract class JobNode extends Node {

    protected Map<String, String> outputs = new HashMap<>();
    protected Long duration;
    protected Date finishedAt;
    protected Integer exitCode;
    protected List<String> logPathes = new ArrayList<>();

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, String> outputs) {
        this.outputs = outputs;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public List<String> getLogPathes() {
        return logPathes;
    }

    public void setLogPathes(List<String> logPathes) {
        this.logPathes = logPathes;
    }
}
