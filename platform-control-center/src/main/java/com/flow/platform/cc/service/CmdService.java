package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public interface CmdService {

    /**
     * Create command from CmdBase
     *
     * @param cmd
     * @return Cmd objc with id
     */
    Cmd create(CmdBase cmd);

    /**
     * Find cmd obj by id
     *
     * @param cmdId
     * @return Cmd object
     */
    Cmd find(String cmdId);

    /**
     * List cmd by agent path
     *
     * @param agentPath
     * @return
     */
    List<Cmd> listByAgentPath(AgentPath agentPath);

    /**
     * Send ZkCmd to agent
     *
     * @param cmd
     * @return command objc with id
     * @exception AgentErr.NotAvailableException if agent busy
     */
    Cmd send(CmdBase cmd);

    /**
     * Update cmd status and result
     *
     * @param cmdId
     * @param status
     * @param result
     */
    void report(String cmdId, Cmd.Status status, CmdResult result);

    /**
     * Record full zipped log to store
     *
     * @param cmdId
     * @param file
     */
    void writeFullLog(String cmdId, MultipartFile file);
}
