package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/cmd")
public class CmdController {

    @Autowired
    private CmdService cmdService;

    @GetMapping(path = "/types")
    public CmdType[] getCmdTypes() {
        return CmdType.values();
    }

    /**
     * Send command to agent
     *
     * @param cmd
     * @return
     */
    @PostMapping(path = "/send", consumes = "application/json")
    public Cmd sendCommand(@RequestBody CmdInfo cmd) {
        return cmdService.send(cmd);
    }

    /**
     * For agent report cmd status
     *
     * @param reportData only need id, status and result
     */
    @PostMapping(path = "/report", consumes = "application/json")
    public void report(@RequestBody CmdReport reportData) {
        if (reportData.getId() == null || reportData.getStatus() == null || reportData.getResult() == null) {
            throw new IllegalArgumentException("Cmd id, status and cmd result are required");
        }
        cmdService.updateStatus(reportData.getId(), reportData.getStatus(), reportData.getResult(), true);
    }

    /**
     * List commands by agent path
     *
     * @param agentPath
     */
    @PostMapping(path = "/list", consumes = "application/json")
    public Collection<Cmd> list(@RequestBody AgentPath agentPath) {
        return cmdService.listByAgentPath(agentPath);
    }

    /**
     * List cmd result by cmd list
     *
     * @param cmdIdList
     * @return
     */
    @PostMapping(path = "/result", consumes = "application/json")
    public Collection<CmdResult> result(@RequestBody Set<String> cmdIdList) {
        return cmdService.listResult(cmdIdList);
    }

    /**
     * Upload zipped cmd log with multipart
     *
     * @param cmdId cmd id with text/plain
     * @param file  zipped cmd log with application/zip
     */
    @PostMapping(path = "/log/upload")
    public void uploadFullLog(@RequestPart String cmdId, @RequestPart MultipartFile file) {
        if (!Objects.equals(file.getContentType(), "application/zip")) {
            throw new IllegalArgumentException("Illegal zipped log file format");
        }
        cmdService.saveLog(cmdId, file);
    }

    /**
     * Get zipped log file by cmd id
     *
     * @param cmdId
     * @return
     */
    @GetMapping(path = "/log/download", produces = "application/zip")
    public Resource downloadFullLog(@RequestParam String cmdId,
                                    @RequestParam Integer index,
                                    HttpServletResponse httpResponse) {

        Cmd cmd = cmdService.find(cmdId);
        if (cmd == null) {
            throw new RuntimeException("Cmd not found");
        }

        try {
            Path filePath = Paths.get(cmd.getLogPaths().get(index));
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            httpResponse.setHeader("Content-Disposition",
                    String.format("attachment; filename=%s", filePath.getFileName().toString()));
            return resource;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Log not found");
        }
    }
}
