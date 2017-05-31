package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Objects;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/cmd")
public class CmdController {

    @Autowired
    private CmdService cmdService;

    /**
     * Send command to agent
     *
     * @param cmd
     * @return
     */
    @RequestMapping(path = "/send", method = RequestMethod.POST, consumes = "application/json")
    public Cmd sendCommand(@RequestBody CmdBase cmd) {
        return cmdService.send(cmd);
    }

    /**
     * For agent report cmd status
     *
     * @param reportData only need id, status and result
     */
    @RequestMapping(path = "/report", method = RequestMethod.POST, consumes = "application/json")
    public void report(@RequestBody CmdReport reportData) {
        if (reportData.getId() == null || reportData.getStatus() == null || reportData.getResult() == null) {
            throw new IllegalArgumentException("Cmd id, status and cmd result are required");
        }
        cmdService.report(reportData.getId(), reportData.getStatus(), reportData.getResult());
    }

    /**
     * List commands by agent path
     *
     * @param agentPath
     */
    @RequestMapping(path = "/list", method = RequestMethod.POST, consumes = "application/json")
    public Collection<Cmd> list(@RequestBody AgentPath agentPath) {
        return cmdService.listByAgentPath(agentPath);
    }

    @RequestMapping(path = "/log/upload", method = RequestMethod.POST)
    public void uploadFullLog(@RequestParam("cmdId") String cmdId,
                              @RequestParam("file") MultipartFile file) {

        if (!Objects.equals(file.getContentType(), "application/zip")) {
            throw new IllegalArgumentException("Illegal zipped log file format");
        }

        cmdService.writeFullLog(cmdId, file);
    }
}
