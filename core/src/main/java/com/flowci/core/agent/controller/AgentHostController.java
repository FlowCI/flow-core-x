/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.agent.controller;

import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.AgentHostAction;
import com.flowci.core.agent.domain.AgentHostOption;
import com.flowci.core.agent.service.AgentHostService;
import com.flowci.core.auth.annotation.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/hosts")
public class AgentHostController {

    @Autowired
    private AgentHostService agentHostService;

    @GetMapping
    @Action(AgentHostAction.LIST)
    public List<AgentHost> list() {
        return agentHostService.list();
    }

    @GetMapping("/{name}")
    @Action(AgentHostAction.GET)
    public AgentHost getByName(@PathVariable String name) {
        return agentHostService.get(name);
    }

    @DeleteMapping("/{name}")
    @Action(AgentHostAction.DELETE)
    public AgentHost deleteByName(@PathVariable String name) {
        return agentHostService.delete(name);
    }

    @PostMapping
    @Action(AgentHostAction.CREATE_UPDATE)
    public AgentHost createOrUpdate(@RequestBody @Validated AgentHostOption body) {
        AgentHost host = body.toObj();
        return agentHostService.createOrUpdate(host);
    }

    @PostMapping("/{name}/test")
    @Action(AgentHostAction.CREATE_UPDATE)
    public void testConnection(@PathVariable String name) {
        AgentHost host = agentHostService.get(name);
        agentHostService.testConn(host);
    }

    @PostMapping("/{name}/switch/{value}")
    @Action(AgentHostAction.CREATE_UPDATE)
    public AgentHost disableOrEnableByName(@PathVariable String name, @PathVariable boolean value) {
        return agentHostService.disableOrEnable(name, value);
    }
}
