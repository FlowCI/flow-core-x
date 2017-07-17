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

    private String id;

    private String type;

    private Date createdAt;

    private Date updatedAt;

    private Long duration;

    private Date finishedAt;

    private Integer exitCode;

    private String nodeName;

    private Map<Env, String> envs = new HashMap<>();

    public Job() {
    }

    public Job(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

        return id.equals(job.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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
