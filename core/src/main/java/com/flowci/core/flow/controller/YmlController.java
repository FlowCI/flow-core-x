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
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.domain.http.RequestMessage;
import com.flowci.tree.StepNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

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

    @GetMapping("/{name}/yml/steps")
    public List<StepNode> listSteps(@PathVariable String name) {
        Flow flow = flowService.get(name);
        return ymlService.ListChildren(flow);
    }

    @PostMapping("/{name}/yml")
    @Action(FlowAction.SET_YML)
    public void setupYml(@PathVariable String name, @RequestBody RequestMessage<String> body) {
        Flow flow = flowService.get(name);
        byte[] yml = Base64.getDecoder().decode(body.getData());
        ymlService.saveYml(flow, new String(yml));
    }

    @GetMapping(value = "/{name}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(FlowAction.GET_YML)
    public String getYml(@PathVariable String name) {
        Flow flow = flowService.get(name);
        String yml = ymlService.getYml(flow).getRaw();
        return Base64.getEncoder().encodeToString(yml.getBytes());
    }
}
