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
import com.flowci.core.common.domain.http.RequestMessage;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.tree.FlowNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class YmlController {

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @GetMapping("/{flowName}/yml/{ymlName}/obj")
    public FlowNode listSteps(@PathVariable String flowName, @PathVariable String ymlName) {
        Flow flow = flowService.get(flowName);
        return ymlService.getRaw(flow.getId(), ymlName);
    }

    @PostMapping("/{flowName}/yml/{ymlName}")
    @Action(FlowAction.SET_YML)
    public void saveYml(@PathVariable String flowName,
                        @PathVariable String ymlName,
                        @RequestBody RequestMessage<String> body) {
        Flow flow = flowService.get(flowName);
        byte[] yml = Base64.getDecoder().decode(body.getData());
        ymlService.saveYml(flow, ymlName, new String(yml));
    }

    @GetMapping(value = "/{flowName}/yml/{ymlName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(FlowAction.GET_YML)
    public String getYml(@PathVariable String flowName, @PathVariable String ymlName) {
        Flow flow = flowService.get(flowName);
        String yml = ymlService.getYmlString(flow.getId(), ymlName);
        return Base64.getEncoder().encodeToString(yml.getBytes());
    }
}
