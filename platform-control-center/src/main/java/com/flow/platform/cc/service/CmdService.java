package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public interface CmdService {

    /**
     * Send ZkCmd to agent
     *
     * @param cmd
     * @return command objc with id
     * @exception AgentErr.NotAvailableException if agent busy
     */
    Cmd send(CmdBase cmd);
}
