/*
 * Copyright 2017 flow.ci
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
package com.flow.platform.api.controller;

import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lhl
 */

@RestController
@RequestMapping(path = "/permissions")
public class PermissionController {

    @Autowired
    private ActionService actionService;

    @GetMapping
    public List<Action> index() {
        return actionService.list();
    }

    @PostMapping
    public Action create(@RequestBody Action action){
        return actionService.create(action);
    }

    @PostMapping(path = "/{action}/delete")
    public void delete(@PathVariable String action) {
        actionService.delete(action);
    }

    @PostMapping(path = "/{action}/update")
    public void update(@PathVariable String action, @RequestBody Action body){
        if (Objects.equals(action, body.getName())) {
            actionService.update(body);
            return;
        }

        throw new IllegalParameterException("The action path doesn't match");
    }
}
