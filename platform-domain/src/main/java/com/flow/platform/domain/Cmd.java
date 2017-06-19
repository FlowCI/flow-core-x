package com.flow.platform.domain;

import com.flow.platform.util.DateUtil;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private String cmdResultId;

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

    public Cmd(CmdBase cmdBase) {
        super(cmdBase.getAgentPath(),
                cmdBase.getType(),
                cmdBase.getCmd());

        this.timeout = cmdBase.getTimeout();
        this.inputs = cmdBase.getInputs();
        this.workingDir = cmdBase.getWorkingDir();
        this.sessionId = cmdBase.getSessionId();
        this.priority = cmdBase.getPriority();
        this.outputEnvFilter = cmdBase.getOutputEnvFilter();
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

    public void setStatus(CmdStatus status) {
        this.status = status;
    }

    public CmdStatus getStatus() {
        return status;
    }

    /**
     * only level gt current level
     * @param status
     */
    public void addStatus(CmdStatus status) {
        if (this.status == null){
            this.status = status;
            return;
        }

        if (this.status.getLevel() < status.getLevel()){
             this.status = status;

            if(FINISH_STATUS.contains(status)){
                this.finishedDate = DateUtil.utcNow();
            }
        }
    }

    public List<String> getLogPaths() {
        return logPaths;
    }

    public void setLogPaths(List<String> logPaths) {
        this.logPaths = logPaths;
    }


    public String getCmdResultId() {
        return cmdResultId;
    }

    public void setCmdResultId(String cmdResultId) {
        this.cmdResultId = cmdResultId;
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
        if(WORKING_STATUS.contains(status)){
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
}
