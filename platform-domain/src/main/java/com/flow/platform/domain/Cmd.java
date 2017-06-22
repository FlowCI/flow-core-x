package com.flow.platform.domain;

import com.flow.platform.util.DateUtil;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Command object to communicate between c/s
 * <p>
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Cmd extends CmdBase {

    /**
     * Working status set
     */
    public static final Set<CmdStatus> WORKING_STATUS =
            Sets.newHashSet(CmdStatus.PENDING, CmdStatus.RUNNING, CmdStatus.EXECUTED);

    /**
     * Finish status set
     */
    public static final Set<CmdStatus> FINISH_STATUS =
            Sets.newHashSet(CmdStatus.LOGGED, CmdStatus.EXCEPTION, CmdStatus.KILLED, CmdStatus.REJECTED, CmdStatus.TIMEOUT_KILL);

    /**
     * Server generated command id
     */
    private String id;

    /**
     * record current status
     */
    private CmdStatus status = CmdStatus.PENDING;

    /**
     * Path for full log
     */
    private List<String> logPaths = new ArrayList<>(5);

    /**
     * Created date
     */
    private Date createdDate;

    /**
     * Updated date
     */
    private Date updatedDate;

    /**
     * finish time
     */
    private Date finishedDate;


    public Cmd() {
    }

    public Cmd(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public Date getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(Date finishedDate) {
        this.finishedDate = finishedDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // DO NOT used in programming to set cmd status, using addStatus instead this func
    public void setStatus(CmdStatus status) {
        this.status = status;
    }

    public CmdStatus getStatus() {
        return status;
    }

    /**
     * only level gt current level
     *
     * @param status target status
     * @return true if status updated
     */
    public boolean addStatus(CmdStatus status) {
        if (this.status == null) {
            this.status = status;
            return true;
        }

        if (this.status.getLevel() < status.getLevel()) {
            this.status = status;

            if(!isCurrent()) {
                this.finishedDate = DateUtil.utcNow();
            }

            return true;
        }

        return false;
    }

    public List<String> getLogPaths() {
        return logPaths;
    }

    public void setLogPaths(List<String> logPaths) {
        this.logPaths = logPaths;
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
        if (WORKING_STATUS.contains(status)) {
            return true;
        }
        return false;
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

    /**
     * Convert CmdBase to Cmd
     *
     * @param base
     * @return
     */
    public static Cmd convert(CmdBase base) {
        Cmd cmd = new Cmd();
        cmd.agentPath = base.getAgentPath();
        cmd.type = base.getType();
        cmd.cmd = base.getCmd();
        cmd.timeout = base.getTimeout();
        cmd.inputs = base.getInputs();
        cmd.workingDir = base.getWorkingDir();
        cmd.sessionId = base.getSessionId();
        cmd.priority = base.getPriority();
        cmd.outputEnvFilter = base.getOutputEnvFilter();
        cmd.webhook = base.getWebhook();
        return cmd;
    }
}
