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

    @PostMapping(value = "/{name}/sourceOfYaml")
    @Action(FlowAction.UPDATE)
    public Flow updateYAMLSource(@PathVariable String name, @RequestBody Settings.UpdateYAMLSource body) {
        Flow flow = flowService.get(name);
        flowSettingService.set(flow, body);
        return flow;
    }

    @PostMapping(value = "/{name}/timeout")
    @Action(FlowAction.UPDATE)
    public Flow updateTimeout(@PathVariable String name, @RequestBody Settings.UpdateTimeout body) {
        Flow flow = flowService.get(name);
        flowSettingService.set(flow, body);
        return flow;
    }

    @PostMapping(value = "/{name}/rename")
    @Action(FlowAction.UPDATE)
    public Flow rename(@PathVariable String name, @RequestBody Settings.RenameFlow body) {
        Flow flow = flowService.get(name);
        flowSettingService.rename(flow, body.getName());
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
