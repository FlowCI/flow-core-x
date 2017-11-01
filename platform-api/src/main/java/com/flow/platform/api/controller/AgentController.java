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
import com.flow.platform.api.domain.SearchCondition;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.events.AgentStatusChangeEvent;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.AgentService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentPathWithPassword;
import com.flow.platform.domain.AgentSettings;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping(path = "/agents")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /**
     * @api {Get} /agents List
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
    @WebSecurity(action = Actions.AGENT_SHOW)
    public List<AgentWithFlow> index() {
        return agentService.list();
    }

    /**
     * @api {Post} /agents Create
     * @apiName Create Agent
     * @apiGroup Agent
     * @apiDescripton Create agent and return agent object with token
     *
     * @apiSuccessExample {String} Success-Response:
     *  HTTP/1.1 200 OK
     *
     *  {
     *        zone: xxx,
     *        name: xxx
     *  }
     */
    @PostMapping(path = "/create")
    @WebSecurity(action = Actions.ADMIN_CREATE)
    public AgentWithFlow create(@RequestBody AgentPath agentPath) {
        if (agentPath.isEmpty()) {
            throw new IllegalParameterException("Zone and agent name are required");
        }

        return agentService.create(agentPath);
    }

    /**
     * @api {Get} /agents/sys/info Agent sys info
     * @apiName Sys info
     * @apiGroup Agent
     * @apiDescription get agent sys info
     * @apiParam {String} zone
     * @apiParam {String} name
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *
     */
    @GetMapping(path = "/sys/info")
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public void agentEnvironmentInfo(@RequestParam Map<String, String> allParams, AgentPath agentPath) {
        if (agentPath.isEmpty()) {
            throw new IllegalParameterException("Zone and agent name are required");
        }
        agentService.sendSysCmd(agentPath);
    }

    /**
     * @api {Get} /agents/settings Agent Settings
     * @apiParam {String} token The agent token via ?token=xxx
     * @apiName Get Agent Settings
     * @apiGroup Agent
     * @apiDescription Get agent settings from control center by token
     *
     * @apiSuccessExample {json} Success-Response:
     *  HTTP/1.1 200 OK
     *
     *  {
     *      agentPath: {
     *          zone: xxx,
     *          name: xxx
     *      },
     *      webSocketUrl: http://xxxx,
     *      cmdStatusUrl: http://xxx,
     *      cmdLogUrl: http://xxxx,
     *      zookeeperUrl: localhost:2181
     *  }
     */
    @GetMapping(path = "/settings")
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public AgentSettings getInfo(@RequestParam String token) {
        if (Strings.isNullOrEmpty(token)) {
            throw new IllegalParameterException("miss required params ");
        }
        return agentService.settings(token);
    }

    /**
     * @api {Post} /agents/close Close
     * @apiName Close Agent
     * @apiParam {json} Request-Body
     *  {
     *      zone: xxx,
     *      name: xxx
     *  }
     * @apiGroup Agent
     * @apiDescription close selected agent
     *
     * @apiSuccessExample {String} Success-Response:
     *  HTTP/1.1 200 OK
     *
     *  {
     *      value: true or false
     *  }
     */
    @PostMapping(path = "/close")
    public BooleanValue close(@RequestBody AgentPath path) {
        if (path.isEmpty()) {
            throw new IllegalParameterException("Agent zone or name are required");
        }

        return new BooleanValue(agentService.close(path));
    }

    /**
     * @api {Post} /agents/shutdown Shutdown
     * @apiName Shutdown Agent
     * @apiParam {json} Request-Body
     *  {
     *      zone: xxx,
     *      name: xxx,
     *      password: xxx
     *  }
     * @apiGroup Agent
     * @apiDescription shutdown host machine on selected agent
     *
     * @apiSuccessExample {String} Success-Response:
     *  HTTP/1.1 200 OK
     *
     *  {
     *      value: true or false
     *  }
     */
    @PostMapping(path = "/shutdown")
    @WebSecurity(action = Actions.ADMIN_DELETE)
    public BooleanValue shutdown(@RequestBody AgentPathWithPassword path) {
        if (path.isEmpty()) {
            throw new IllegalParameterException("Agent zone or name are required");
        }

        return new BooleanValue(agentService.shutdown(path, path.getPassword()));
    }

    /**
     * @api {Post} /agents/callback Callback
     * @apiName AgentCallback
     * @apiParam {json} Request-Body
     *  {
     *      path: {
     *          zone: xxx,
     *          name: xxx
     *      },
     *      concurrentProc: 1,
     *      status: IDLE,
     *      sessionId: xxxxx,
     *      sessionDate: xxx,
     *      token: xxxx,
     *      createdDate: xxx,
     *      updatedDate: xxx
     *  }
     * @apiGroup Agent
     * @apiDescription: Callback API for agent webhook when agent status changed
     */
    @PostMapping(path = "/callback")
    public void callback(@RequestBody Agent agent) {
        agentService.onAgentStatusChange(agent);
    }

    /**
     * @api {Post} /agents/delete Delete
     * @apiName Delete Agent
     * @apiGroup Agent
     * @apiDescription Delete agent by agentPath
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         zone: xxx,
     *         name: xxx
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping(path = "/delete")
    @WebSecurity(action = Actions.ADMIN_DELETE)
    public void delete(@RequestBody AgentPath agentPath) {
        agentService.delete(agentPath);
    }
}
