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

import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.util.Logger;
import java.util.List;
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
@RequestMapping(path = "/roles")
public class RoleController {

    private final static Logger LOGGER = new Logger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @GetMapping
    public List<Role> index() {
        return roleService.listRoles();
    }

    @PostMapping
    public Role create(@RequestBody Role role){
        return roleService.create(role);
    }

    @GetMapping(path = "/{name}/delete")
    public void delete(@PathVariable String name) {
        roleService.delete(name);
    }

    @PostMapping(path = "/{id}/update")
    public Role update(@RequestBody Role role){
        return roleService.update(role);
    }
}
