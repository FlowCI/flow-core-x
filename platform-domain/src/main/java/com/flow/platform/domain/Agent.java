package com.flow.platform.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class Agent implements Serializable {

    public enum Status {
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

    /**
     * Agent target working zone
     */
    private String zone;

    /**
     * Agent name, unique inside zone
     */
    private String name;

    /**
     * Max concurrent proc number
     */
    private Integer concurrentProc = 1;

    /**
     * Agent busy or idle
     */
    private Status status = Status.IDLE;

    /**
     * Created date
     */
    private Date createdDate;

    /**
     * Updated date
     */
    private Date updatedDate;

    public Agent(String zone, String name) {
        this.zone = zone;
        this.name = name;
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

        if (!zone.equals(agent.zone)) return false;
        return name.equals(agent.name);
    }

    @Override
    public int hashCode() {
        int result = zone.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "zone='" + zone + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
