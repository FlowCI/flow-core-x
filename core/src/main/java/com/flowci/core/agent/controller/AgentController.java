/*
 * Copyright 2018 flow.ci
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

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentAction;
import com.flowci.core.agent.domain.AgentOption;
import com.flowci.core.agent.domain.DeleteAgent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.auth.annotation.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yang
 */
@Slf4j
@RestController
@RequestMapping("/agents")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @GetMapping("/{name}")
    @Action(AgentAction.GET)
    public Agent getByName(@PathVariable String name) {
        return agentService.getByName(name);
    }

    @GetMapping
    @Action(AgentAction.LIST)
    public List<Agent> list() {
        return agentService.list();
    }

    @PostMapping()
    @Action(AgentAction.CREATE_UPDATE)
    public Agent createOrUpdate(@Validated @RequestBody AgentOption body) {
        if (body.hasToken()) {
            return agentService.update(body);
        }

        return agentService.create(body);
    }

    @DeleteMapping()
    @Action(AgentAction.DELETE)
    public Agent delete(@Validated @RequestBody DeleteAgent body) {
        return agentService.delete(body.getToken());
    }
}
