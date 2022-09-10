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

package com.flowci.core.flow.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.*;
import com.flowci.core.flow.service.FlowGroupService;
import com.flowci.core.flow.service.FlowItemService;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.google.common.collect.Lists;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class FlowController {

    private final List<Template> templates;

    private final UserService userService;

    private final FlowService flowService;

    private final FlowGroupService flowGroupService;

    private final FlowItemService flowItemService;

    public FlowController(List<Template> templates,
                          UserService userService,
                          FlowService flowService,
                          FlowGroupService flowGroupService,
                          FlowItemService flowItemService) {
        this.templates = templates;
        this.userService = userService;
        this.flowService = flowService;
        this.flowGroupService = flowGroupService;
        this.flowItemService = flowItemService;
    }

    @GetMapping
    @Action(FlowAction.LIST)
    public List<FlowItem> list() {
        return flowItemService.list();
    }

    @GetMapping("/templates")
    @Action(FlowAction.LIST)
    public List<Template> getTemplates() {
        return templates;
    }

    @GetMapping(value = "/{name}")
    @Action(FlowAction.GET)
    public Flow get(@PathVariable String name, @RequestParam boolean group) {
        var flow = flowService.get(name);
        if (group && flow.hasParentId()) {
            flow.setParent(flowGroupService.getById(flow.getParentId()));
        }
        return flow;
    }

    @GetMapping(value = "/{name}/exist")
    @Action(FlowAction.CHECK_NAME)
    public Boolean exist(@PathVariable String name) {
        return flowItemService.existed(name);
    }

    @PostMapping(value = "/{name}")
    @Action(FlowAction.CREATE)
    public Flow create(@PathVariable String name, @RequestBody CreateOption option) {
        return flowService.create(name, option);
    }

    @DeleteMapping("/{name}")
    @Action(FlowAction.DELETE)
    public Flow delete(@PathVariable String name) {
        var flow = flowService.get(name);
        flowService.delete(flow);
        return flow;
    }

    /**
     * Create credential for flow only
     */
    @PostMapping("/{name}/secret/rsa")
    @Action(FlowAction.SETUP_CREDENTIAL)
    public String setupRSACredential(@PathVariable String name, @RequestBody SimpleKeyPair pair) {
        return flowService.setSshRsaCredential(name, pair);
    }

    @PostMapping("/{name}/secret/auth")
    @Action(FlowAction.SETUP_CREDENTIAL)
    public String setupAuthCredential(@PathVariable String name, @RequestBody SimpleAuthPair pair) {
        return flowService.setAuthCredential(name, pair);
    }

    @PostMapping("/{name}/users")
    @Action(FlowAction.ADD_USER)
    public List<User> addUsers(@PathVariable String name, @RequestBody String[] emails) {
        Flow flow = flowService.get(name);
        flowService.addUsers(flow, emails);
        return userService.list(Lists.newArrayList(emails));
    }

    @DeleteMapping("/{name}/users")
    @Action(FlowAction.REMOVE_USER)
    public List<User> removeUsers(@PathVariable String name, @RequestBody String[] emails) {
        Flow flow = flowService.get(name);
        flowService.removeUsers(flow, emails);
        return userService.list(Lists.newArrayList(emails));
    }

    @GetMapping("/{name}/users")
    @Action(FlowAction.LIST_USER)
    public List<User> listUsers(@PathVariable String name) {
        List<String> emails = flowService.listUsers(name);
        return userService.list(emails);
    }

    @GetMapping("/secret/{name}")
    @Action(FlowAction.LIST_BY_CREDENTIAL)
    public List<Flow> listFlowByCredentials(@PathVariable String name) {
        return flowService.listByCredential(name);
    }
}
