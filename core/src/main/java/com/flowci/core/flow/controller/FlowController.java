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
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.domain.OnConfirm;
import com.flowci.core.flow.domain.Template;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class FlowController {

    @Autowired
    private List<Template> templates;

    @Autowired
    private UserService userService;

    @Autowired
    private FlowService flowService;

    @GetMapping
    @Action(FlowAction.LIST)
    public List<Flow> list() {
        return flowService.list(Status.CONFIRMED);
    }

    @GetMapping("/templates")
    @Action(FlowAction.LIST)
    public List<Template> getTemplates() {
        return templates;
    }

    @GetMapping(value = "/{name}")
    @Action(FlowAction.GET)
    public Flow get(@PathVariable String name) {
        return flowService.get(name);
    }

    @GetMapping(value = "/{name}/exist")
    @Action(FlowAction.CHECK_NAME)
    public Boolean exist(@PathVariable String name) {
        return flowService.exist(name);
    }

    @PostMapping(value = "/{name}")
    @Action(FlowAction.CREATE)
    public Flow create(@PathVariable String name) {
        return flowService.create(name);
    }

    @PostMapping(value = "/{name}/confirm")
    @Action(FlowAction.CONFIRM)
    public Flow confirm(@PathVariable String name, @RequestBody(required = false) OnConfirm onConfirm) {
        if (Objects.isNull(onConfirm)) {
            onConfirm = new OnConfirm();
        }
        return flowService.confirm(name, onConfirm.getGitUrl(), onConfirm.getSecret());
    }

    @DeleteMapping("/{name}")
    @Action(FlowAction.DELETE)
    public Flow delete(@PathVariable String name) {
        return flowService.delete(name);
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
        Flow flow = flowService.get(name);
        List<String> emails = flowService.listUsers(flow);
        return userService.list(emails);
    }

    @GetMapping("/secret/{name}")
    @Action(FlowAction.LIST_BY_CREDENTIAL)
    public List<Flow> listFlowByCredentials(@PathVariable String name) {
        return flowService.listByCredential(name);
    }
}
