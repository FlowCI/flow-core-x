package com.flow.platform.domain;

import java.time.ZonedDateTime;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class Agent extends Jsonable {

    /**
     * Composite key
     */
    private AgentPath path;

    /**
     * Max concurrent proc number
     */
    private Integer concurrentProc = 1;

    /**
     * Agent busy or idle
     */
    private AgentStatus status = AgentStatus.OFFLINE;

    /**
     * Reserved for session id
     */
    private String sessionId;

    /**
     * The date to start session
     */
    private ZonedDateTime sessionDate;

    /**
     * Created date
     */
    private ZonedDateTime createdDate;

    /**
     * Updated date
     */
    private ZonedDateTime updatedDate;

    public Agent() {
    }

    public Agent(String zone, String name) {
        this(new AgentPath(zone, name));
    }

    public Agent(AgentPath path) {
        this.path = path;
        this.createdDate = ZonedDateTime.now();
        this.updatedDate = ZonedDateTime.now();
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

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ZonedDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(ZonedDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(ZonedDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public boolean isAvailable() {
        return getStatus() == AgentStatus.IDLE;
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
                ", status=" + status + '\'' +
                ", sessionId=" + sessionId +
                '}';
    }
}
