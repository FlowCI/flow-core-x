package com.flowci.core.flow.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.domain.Settings;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.FlowSettingService;
import com.flowci.domain.VarValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flows")
public class SettingController {

    @Autowired
    private FlowService flowService;

    @Autowired
    private FlowSettingService flowSettingService;

    @PostMapping(value = "/{name}/settings")
    @Action(FlowAction.UPDATE)
    public Flow updateSettings(@PathVariable String name, @Validated @RequestBody Settings body) {
        Flow flow = flowService.get(name);
        flowSettingService.set(flow, body);
        return flow;
    }

    @PostMapping("/{name}/variables")
    @Action(FlowAction.UPDATE)
    public void addVariables(@PathVariable String name,
                             @Validated @RequestBody Map<String, VarValue> variables) {
        Flow flow = flowService.get(name);
        flowSettingService.add(flow, variables);
    }

    @DeleteMapping("/{name}/variables")
    @Action(FlowAction.UPDATE)
    public void removeVariables(@PathVariable String name, @RequestBody List<String> vars) {
        Flow flow = flowService.get(name);
        flowSettingService.remove(flow, vars);
    }
}
