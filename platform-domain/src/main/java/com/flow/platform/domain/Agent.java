package com.flow.platform.domain;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class Agent implements Serializable {

    public enum Status {

        OFFLINE("OFFLINE"),

        IDLE("IDLE"),

        BUSY("BUSY");

        private String name;

        Status(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private AgentPath path;

    /**
     * Max concurrent proc number
     */
    private Integer concurrentProc = 1;

    /**
     * Agent busy or idle
     */
    private Status status = Status.OFFLINE;

    /**
     * Created date
     */
    private Date createdDate;

    /**
     * Updated date
     */
    private Date updatedDate;

    public Agent(String zone, String name) {
        this(new AgentPath(zone, name));
    }

    public Agent(AgentPath path) {
        this.path = path;
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    public AgentPath getPath() {
        return path;
    }

    public void setPath(AgentPath path) {
        this.path = path;
    }

    public String getZone() {
        return this.path.getZone();
    }

    public String getName() {
        return this.path.getName();
    }

    public Integer getConcurrentProc() {
        return concurrentProc;
    }

    public void setConcurrentProc(Integer concurrentProc) {
        this.concurrentProc = concurrentProc;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Agent agent = (Agent) o;
        return path.equals(agent.getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    @Override
    public String toString() {
        return "Agent{" +
                "zone='" + path.getZone() + '\'' +
                ", name='" + path.getName() + '\'' +
                ", status=" + status +
                '}';
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
