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

import com.flow.platform.api.domain.AgentWithFlow;
import com.flow.platform.api.service.AgentService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.AgentPath;
import com.google.common.base.Strings;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping(path = "/agents")
public class AgentsController {

    @Autowired
    private AgentService agentService;

    /**
     * @api {Get} /agents list
     * @apiName AgentList
     * @apiGroup Agent
     * @apiDescription get agent list
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *     [
     *          {
     *              "name": "fir-machine-a",
     *              "zone": "default",
     *              "agentStatus": "OFFLINE",
     *              "zoneWithName": "fir-machine-a - default"
     *          }
     *      ]
     *
     */
    @GetMapping
    public List<AgentWithFlow> index(){
        return agentService.list();
    }


    /**
     * @api {Post} /agents/shutdown shutdown
     * @apiName AgentShutdown
     * @apiParam {String} [zone] agent zone name
     * @apiParam {String} [name] agent name
     * @apiParam {String} [password] machine password
     * @apiGroup Agent
     * @apiDescription shutdown agent
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *      true or false
     */
    @PostMapping(path = "/shutdown")
    public boolean shutDown(@RequestParam String zone, @RequestParam String name,
        @RequestParam(required = false) String password) {
        if(Strings.isNullOrEmpty(zone) || Strings.isNullOrEmpty(name)){
            throw new IllegalParameterException("require zone or name not found");
        }
        Boolean t = agentService.shutdown(zone, name, password);
        return t;
    }
}
