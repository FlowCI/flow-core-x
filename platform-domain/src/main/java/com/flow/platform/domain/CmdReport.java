package com.flow.platform.domain;

/**
 * For report cmd status and result
 *
 * Created by gy@fir.im on 30/05/2017.
 * Copyright fir.im
 */
public class CmdReport extends Jsonable {

    // cmd id
    private String id;

    // reported status
    private Cmd.Status status;

    // reported result
    private CmdResult result;

    public CmdReport() {
    }

    public CmdReport(String id, Cmd.Status status, CmdResult result) {
        this.id = id;
        this.status = status;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Cmd.Status getStatus() {
        return status;
    }

    public void setStatus(Cmd.Status status) {
        this.status = status;
    }

    public CmdResult getResult() {
        return result;
    }

    public void setResult(CmdResult result) {
        this.result = result;
    }
}
