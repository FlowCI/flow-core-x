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
import com.flow.platform.api.domain.request.ActionParam;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lhl
 */

@RestController
@RequestMapping(path = "/actions")
public class ActionController {

    @Autowired
    private ActionService actionService;

    /**
     * @api {get} /actions List
     * @apiGroup Action
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          name: FLOW_CREATE,
     *          alias: crate flow,
     *          description: xxxx,
     *          tag: ADMIN,
     *          createdAt: xxx,
     *          updatedAt: xxx
     *      }
     *  ]
     */
    @GetMapping
    public List<Action> index() {
        return actionService.list();
    }

    /**
     * @api {patch} /actions/:name Update
     * @apiParam {String} name Update action by name
     * @apiParamExample {json} Request-Body
     *  {
     *      alias: xxxx,
     *      description: xxx,
     *      tag: USER
     *  }
     * @apiGroup Action
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      name: FLOW_CREATE,
     *      alias: create flow,
     *      description: xxxx,
     *      tag: ADMIN,
     *      createdAt: xxx,
     *      updatedAt: xxx
     *  }
     */
    @PatchMapping(path = "/{name}")
    public Action update(@PathVariable String name, @RequestBody ActionParam body) {
        return actionService.update(name, body);
    }
}
