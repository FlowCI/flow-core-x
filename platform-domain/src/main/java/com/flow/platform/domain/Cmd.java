package com.flow.platform.domain;

import com.google.common.collect.Sets;

import java.util.Date;
import java.util.Set;

/**
 * Command object to communicate between c/s
 *
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Cmd extends CmdBase {

    /**
     * Working status set
     */
    public static final Set<Status> WORKING_STATUS =
            Sets.newHashSet(Status.PENDING, Status.RUNNING, Status.EXECUTED);

    /**
     * Finish status set
     */
    public static final Set<Status> FINISH_STATUS =
            Sets.newHashSet(Status.LOGGED, Status.EXCEPTION, Status.KILLED, Status.REJECTED);

    /**
     * Status for TYPE.RUN_SHELL
     */
    public enum Status {

        /**
         * Init status when cmd prepare send to agent
         * is_current_cmd = true
         */
        PENDING("PENDING"),

        /**
         * Cmd is running
         * is_current_cmd = true
         */
        RUNNING("RUNNING"), // current cmd

        /**
         * Cmd executed but not finish logging
         * is_current_cmd = true
         */
        EXECUTED("EXECUTED"), // current cmd

        /**
         * Log uploaded, cmd completely finished
         * is_current_cmd = false
         */
        LOGGED("LOGGED"),

        /**
         * Got exception when running
         * is_current_cmd = false
         */
        EXCEPTION("EXCEPTION"),

        /**
         * Killed by controller
         * is_current_cmd = false
         */
        KILLED("KILLED"),

        /**
         * Cannot execute since over agent limit
         * is_current_cmd = false
         */
        REJECTED("REJECTED");

        private String name;

        Status(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Server generated command id
     */
    private String id;

    /**
     * Cmd status set
     */
    private Set<Status> statusSet = Sets.newHashSet(Status.PENDING);

    /**
     * Path for full log
     */
    private String fullLogPath;

    /**
     * Cmd execution result
     */
    private CmdResult result;

    /**
     * Created date
     */
    private Date createdDate;

    /**
     * Updated date
     */
    private Date updatedDate;


    public Cmd() {
    }

    public Cmd(CmdBase cmdBase) {
        super(cmdBase.getAgentPath(),
                cmdBase.getType(),
                cmdBase.getCmd());
    }

    public Cmd(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Status> getStatus() {
        return statusSet;
    }

    public void addStatus(Status status) {
        statusSet.add(status);
    }

    public String getFullLogPath() {
        return fullLogPath;
    }

    public void setFullLogPath(String fullLogPath) {
        this.fullLogPath = fullLogPath;
    }

    public CmdResult getResult() {
        return result;
    }

    public void setResult(CmdResult result) {
        this.result = result;
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

    public Boolean isCurrent() {
        // check status set has finished status
        Sets.SetView<Status> intersection = Sets.intersection(statusSet, FINISH_STATUS);
        return intersection.size() <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Cmd cmd = (Cmd) o;

        return id != null ? id.equals(cmd.id) : cmd.id == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cmd{" +
                "id='" + id + '\'' +
                ", info=" + super.toString() +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                '}';
    }
}
