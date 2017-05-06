package com.flow.platform.domain;

/**
 * Created by gy@fir.im on 06/05/2017.
 *
 * @copyright fir.im
 */
public enum ClientCommand {

    /**
     * Run node task
     */
    RUN("RUN"),

    /**
     * Stop node task
     */
    STOP("STOP");

    private String cmd;

    ClientCommand(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}
