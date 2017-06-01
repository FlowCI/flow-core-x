package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.domain.Agent;
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
        return agentService.onlineList(zoneName);
    }

    /**
     * Update agent status, required attributes are
     *  - path
     *  - status
     *
     * @param agent agent objc
     */
    @RequestMapping(path= "/report", method = RequestMethod.POST, consumes = "application/json")
    public void reportStatus(@RequestBody Agent agent) {
        if (agent.getPath() == null || agent.getStatus() == null) {
            throw new IllegalArgumentException("Agent path and status are required");
        }
        agentService.reportStatus(agent.getPath(), agent.getStatus());
    }
}
