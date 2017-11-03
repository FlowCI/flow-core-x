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

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.RoleParam;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.Logger;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ActionService actionService;

    /**
     * @api {get} /roles List
     * @apiGroup Role
     * @apiDescription Get role list
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          id: 1,
     *          name: ROLE_ADMIN,
     *          description: xxxx,
     *          createBy: xxxx
     *      }
     *  ]
     */
    @GetMapping
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public List<Role> index() {
        return roleService.list();
    }

    /**
     * @api {post} /roles Create
     * @apiParamExample {json} Request-Body
     *  {
     *      name: ROLE_ADMIN,
     *      description: xxx
     *  }
     * @apiGroup Role
     * @apiDescription Create role with name and descrption
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      id: 1,
     *      name: ROLE_ADMIN,
     *      description: xxxx,
     *      createBy: xxxx
     *  }
     */
    @PostMapping
    @WebSecurity(action = Actions.ADMIN_CREATE)
    public Role create(@RequestBody RoleParam role) {
        return roleService.create(role.getName(), role.getDescription());
    }

    /**
     * @api {delete} /roles/:id Delete
     * @apiParam {Integer} id Role id
     * @apiGroup Role
     * @apiDescritpion Delete role by id, will 400 error if role has user assigned
     *
     * @apiSuccessExample {json} Success-Response
     *  HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response
     *  HTTP/1.1 400 BAD REQUEST
     *
     *  {
     *      message: xxxx
     *  }
     */
    @DeleteMapping(path = "/{id}")
    @WebSecurity(action = Actions.ADMIN_DELETE)
    public void delete(@PathVariable(name = "id") Integer roleId) {
        roleService.delete(roleId);
    }

    /**
     * @api {patch} /roles/:id Update
     * @apiParam {Integer} id Role id
     * @apiParamExample {josn} Request-Body
     *  {
     *      name: new name,
     *      description: xxx
     *  }
     * @apiGroup Role
     * @apiDescritpion Update role by id
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      id: 1,
     *      name: ROLE_ADMIN,
     *      description: xxxx,
     *      createBy: xxxx
     *  }
     */
    @PatchMapping(path = "/{id}")
    @WebSecurity(action = Actions.ADMIN_UPDATE)
    public Role update(@PathVariable(name = "id") Integer roleId, @RequestBody RoleParam role) {
        Role roleObj = roleService.find(roleId);
        roleObj.setName(role.getName());
        roleObj.setDescription(role.getDescription());

        roleService.update(roleObj);
        return roleObj;
    }

    /**
     * @api {put} /roles/:id/assign Assign Action
     * @apiParam {Integer} id Role id
     * @apiParamExample {josn} Request-Body
     *  [
     *      FLOW_CREATE,
     *      FLOW_DELETE
     *  ]
     * @apiGroup Role
     * @apiDescritpion Assign actions to role
     */
    @PutMapping(path = "/{id}/assign")
    public void assign(@PathVariable(name = "id") Integer roleId, @RequestBody List<String> actions) {
        List<Action> actionList = actionService.list(actions);
        Role role = roleService.find(roleId);
        permissionService.assign(role, actionList);
    }

    /**
     * @api {put} /roles/:id/unassign Remove Action
     * @apiParam {Integer} id Role id
     * @apiParamExample {josn} Request-Body
     *  [
     *      FLOW_CREATE,
     *      FLOW_DELETE
     *  ]
     * @apiGroup Role
     * @apiDescritpion Remove actions from role
     */
    @PutMapping(path = "/{id}/unassign")
    public void unAssign(@PathVariable(name = "id") Integer roldId, @RequestBody List<String> actions) {
        List<Action> actionList = actionService.list(actions);
        Role role = roleService.find(roldId);
        permissionService.unAssign(role, actionList);
    }
}
