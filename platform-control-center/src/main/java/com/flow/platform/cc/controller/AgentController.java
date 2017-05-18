package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.ZkService;
import com.flow.platform.util.zk.ZkCmd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final ZkService zkService;

    @Autowired
    public AgentController(ZkService zkService) {
        this.zkService = zkService;
    }

    /**
     * List online agents by zone name
     */
    @RequestMapping(path = "/list", method = RequestMethod.GET)
    public Collection<String> list(@RequestParam(name = "zone") String zoneName) {
        return zkService.onlineAgent(zoneName);
    }

    @RequestMapping(path = "/cmd", method = RequestMethod.POST, consumes = "application/json")
    public void sendCommand(@RequestParam(name = "zone") String zoneName,
                            @RequestParam(name = "agent") String agentName,
                            @RequestBody ZkCmd cmd) {
        zkService.sendCommand(zoneName, agentName, cmd);
    }
}
