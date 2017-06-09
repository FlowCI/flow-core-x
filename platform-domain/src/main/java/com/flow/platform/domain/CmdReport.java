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
    private CmdStatus status;

    // reported result
    private CmdResult result;

    public CmdReport() {
    }

    public CmdReport(String id, CmdStatus status, CmdResult result) {
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

    public CmdStatus getStatus() {
        return status;
    }

    public void setStatus(CmdStatus status) {
        this.status = status;
    }

    public CmdResult getResult() {
        return result;
    }

    public void setResult(CmdResult result) {
        this.result = result;
    }
}
