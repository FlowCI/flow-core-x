package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
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

    private final AgentService agentService;

    @Autowired
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * List online agents by zone name
     */
    @RequestMapping(path = "/list", method = RequestMethod.GET)
    public Collection<Agent> list(@RequestParam(name = "zone") String zoneName) {
        return agentService.onlineAgent(zoneName);
    }

    @RequestMapping(path = "/cmd", method = RequestMethod.POST, consumes = "application/json")
    public Cmd sendCommand(@RequestBody CmdBase cmd) {
        return agentService.sendCommand(cmd);
    }
}
