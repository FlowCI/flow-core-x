package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
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
    @GetMapping(path = "/list")
    public Collection<Agent> list(@RequestParam(name = "zone") String zoneName) {
        return agentService.onlineList(zoneName);
    }

    @GetMapping(path = "/find")
    public Agent find(@RequestParam(name = "zone") String zoneName,
                      @RequestParam(name = "name") String agentName) {
        return agentService.find(new AgentPath(zoneName, agentName));
    }

    /**
     * Update agent status, required attributes are
     *  - path
     *  - status
     *
     * @param agent agent objc
     */
    @PostMapping(path= "/report", consumes = "application/json")
    public void reportStatus(@RequestBody Agent agent) {
        if (agent.getPath() == null || agent.getStatus() == null) {
            throw new IllegalArgumentException("Agent path and status are required");
        }
        agentService.reportStatus(agent.getPath(), agent.getStatus());
    }
}
