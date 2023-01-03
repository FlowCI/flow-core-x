/*
 * Copyright 2019 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.flow.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.tree.FlowNode;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
@AllArgsConstructor
public class YmlController {

    private FlowService flowService;

    private final YmlService ymlService;

    @GetMapping("/{flowName}/yml")
    public FlowYml get(@PathVariable String flowName) {
        Flow flow = flowService.get(flowName);
        return ymlService.get(flow.getId());
    }

    @GetMapping("/{flowName}/yml/steps")
    public FlowNode steps(@PathVariable String flowName) {
        Flow flow = flowService.get(flowName);
        return ymlService.getTree(flow.getId()).getRoot();
    }

    @PostMapping("/{flowName}/yml")
    @Action(FlowAction.SET_YML)
    public void saveYml(@PathVariable String flowName, @RequestBody List<SimpleYml> body) {
        Flow flow = flowService.get(flowName);
        ymlService.saveYml(flow, body);
    }
}
