package com.flowci.core.flow.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.domain.FlowGroup;
import com.flowci.core.flow.service.FlowGroupService;
import com.flowci.core.flow.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flow_groups")
public class FlowGroupController {

    private final FlowService flowService;
    private final FlowGroupService flowGroupService;

    @Autowired
    public FlowGroupController(FlowService flowService, FlowGroupService flowGroupService) {
        this.flowService = flowService;
        this.flowGroupService = flowGroupService;
    }

    @PostMapping("/{name}")
    @Action(FlowAction.GROUP_UPDATE)
    public FlowGroup create(@PathVariable String name) {
        return flowGroupService.create(name);
    }

    @PostMapping("/{groupName}/{flowName}")
    @Action(FlowAction.GROUP_UPDATE)
    public void addToGroup(@PathVariable String groupName, @PathVariable String flowName) {
        flowGroupService.addToGroup(flowName, groupName);
    }

    @DeleteMapping("/{name}")
    @Action(FlowAction.GROUP_UPDATE)
    public void delete(@PathVariable String name, @RequestParam(required = false) boolean deleteFlow) {
        var group = flowGroupService.get(name);

        if (deleteFlow) {
            var flowList = flowGroupService.flows(group.getId());
            for (var flow : flowList) {
                flowService.delete(flow);
            }
        }

        flowGroupService.delete(group.getName());
    }
}
