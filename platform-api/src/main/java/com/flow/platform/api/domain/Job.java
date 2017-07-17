/*
 * *
 *  * Created by yh@fir.im
 *  * Copyright fir.im
 *
 */

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Job extends Jsonable {

    protected String type;
    protected Date createdAt;
    protected Date updatedAt;
    protected Long duration;
    protected Date finishedAt;
    protected Integer exitCode;
    protected String nodeName;
    protected Map<Env, String> envs = new HashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Map<Env, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<Env, String> envs) {
        this.envs = envs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Job job = (Job) o;

        if (type != null ? !type.equals(job.type) : job.type != null) {
            return false;
        }
        if (createdAt != null ? !createdAt.equals(job.createdAt) : job.createdAt != null) {
            return false;
        }
        if (updatedAt != null ? !updatedAt.equals(job.updatedAt) : job.updatedAt != null) {
            return false;
        }
        if (duration != null ? !duration.equals(job.duration) : job.duration != null) {
            return false;
        }
        if (finishedAt != null ? !finishedAt.equals(job.finishedAt) : job.finishedAt != null) {
            return false;
        }
        if (exitCode != null ? !exitCode.equals(job.exitCode) : job.exitCode != null) {
            return false;
        }
        if (nodeName != null ? !nodeName.equals(job.nodeName) : job.nodeName != null) {
            return false;
        }
        return envs != null ? envs.equals(job.envs) : job.envs == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (finishedAt != null ? finishedAt.hashCode() : 0);
        result = 31 * result + (exitCode != null ? exitCode.hashCode() : 0);
        result = 31 * result + (nodeName != null ? nodeName.hashCode() : 0);
        result = 31 * result + (envs != null ? envs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Job{" +
            "createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", duration=" + duration +
            ", finishedAt=" + finishedAt +
            ", exitCode=" + exitCode +
            ", nodeName='" + nodeName + '\'' +
            '}';
    }
}
