package com.flow.platform.cmd;

import com.flow.platform.domain.CmdResult;

/**
 * Created by gy@fir.im on 20/05/2017.
 * Copyright fir.im
 */
public interface ProcListener {

    /**
     * Proc start to exec
     *
     * @param result
     */
    void onStarted(CmdResult result);

    /**
     * Proc log processed
     *
     * @param result
     */
    void onLogged(CmdResult result);

    /**
     * Proc executed without exception (option)
     *
     * @param result
     */
    void onExecuted(CmdResult result);

    /**
     * Proc got exception while executing (option)
     *
     * @param result
     */
    void onException(CmdResult result);
}
