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
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.request.FlowAuthUser;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
@RequestMapping(path = "/flows")
public class FlowController extends NodeController {

    @Autowired
    private YmlService ymlService;

    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Flow> index() {
        return nodeService.listFlows();
    }

    /**
     * @api {get} /flows/:root Show
     * @apiParam {String} root flow node name
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_1: xxxx,
     *          FLOW_ENV_2: xxxx
     *      }
     *  }
     */
    @GetMapping(path = {"/{root}", "/{root}/show"})
    @WebSecurity(action = Actions.FLOW_SHOW)
    public Node show() {
        String path = getNodePathFromUrl();
        Node node = nodeService.find(path);
        if (node == null) {
            throw new IllegalParameterException(String.format("The flow name %s doesn't exist", path));
        }
        return node;
    }

    /**
     * @api {post} /flows/:root Create
     * @apiParam {String} root flow node name will be created
     * @apiDescription Create empty flow node with default env variables
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_STATUS: PENDING,
     *          FLOW_GIT_WEBHOOK: http://xxx,
     *          FLOW_YML_STATUS: NOT_FOUND
     *      }
     *  }
     */
    @PostMapping(path = {"/{root}", "/{root}/create"})
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Node createEmptyFlow() {
        String path = getNodePathFromUrl();
        return nodeService.createEmptyFlow(path);
    }

    /**
     * @api {post} /flows/:root/delete Delete
     * @apiParam {String} root flow node name will be deleted
     * @apiDescription Delete flow node by name and return flow node object
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @PostMapping(path = "/{root}/delete")
    @WebSecurity(action = Actions.FLOW_DELETE)
    public Node delete() {
        String path = getNodePathFromUrl();
        return nodeService.delete(path);
    }

    /**
     * @api {post} /flows/:root/env Set Env Variables
     * @apiParam {String} root flow node name will be set env variables
     * @apiParamExample {json} Request-Body:
     *  {
     *      FLOW_ENV_VAR_2: xxx,
     *      FLOW_ENV_VAR_1: xxx
     *  }
     * @apiGroup Flows
     * @apiDescription Write env variables to flow env variables, overwrite if env existed
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @PostMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Node setFlowEnv(@RequestBody Map<String, String> envs) {
        String path = getNodePathFromUrl();
        return nodeService.setFlowEnv(path, envs);
    }

    /**
     * @api {get} /flows/:rootenv/:key Get Env
     * @apiParam {String} root root node name
     * @apiParam {String} [key] env variable name
     * @apiGroup Flows
     * @apiDescription Get node env by path or name
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      FLOW_ENV_VAR: xxx
     *  }
     */
    @GetMapping(path = "/{root}/env/{key}")
    @WebSecurity(action = Actions.FLOW_SHOW)
    public Map<String, String> getFlowEnv(@PathVariable(required = false) String key) {
        return super.getEnv(key);
    }

    /**
     * @api {get} /flows/:root/exist IsExisted
     * @apiParam {String} root flow node name to check
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      value: true
     *  }
     */
    @GetMapping("/{root}/exist")
    @WebSecurity(action = Actions.FLOW_SHOW)
    public BooleanValue isFlowNameExist() {
        String path = getNodePathFromUrl();
        boolean exist = nodeService.exist(path);
        return new BooleanValue(exist);
    }

    /**
     * @api {get} /flows/webhooks Webhooks
     * @apiGroup Flows
     * @apiDescription List all web hooks of flow
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          path: /flow-path,
     *          hook: http://xxx.hook.url
     *      }
     *  ]
     */
    @GetMapping("/webhooks")
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Webhook> listFlowWebhooks() {
        return nodeService.listWebhooks();
    }

    /**
     * @api {get} /flows/:root/yml Get
     * @apiParam {String} root flow node name of yml
     * @apiGroup Flow Yml
     * @apiDescription Get flow node related yml content,
     * response empty yml content if it is loading from git repo
     *
     * @apiSuccessExample {yaml} Success-Response
     *  - flows
     *      - name: xx
     *      - steps:
     *          - name: xxx
     *
     * @apiSuccessExample {yaml} GitLoading-Response
     *  Empty yml content
     */
    @GetMapping(value = "/{root}/yml")
    @WebSecurity(action = Actions.FLOW_SHOW)
    public String getRawYml() {
        String path = getNodePathFromUrl();
        return ymlService.getYmlContent(path);
    }

    /**
     * @api {get} /flows/:root/yml/load Load
     * @apiParam {String} root flow node name to load yml
     * @apiGroup Flow Yml
     * @apiDescription Async to load yml content from git repo,
     * the env variable FLOW_GIT_SOURCE and FLOW_GIT_URL variables are required,
     * otherwise it will response 400 error
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @GetMapping("/{root}/yml/load")
    @WebSecurity(action = Actions.FLOW_YML)
    public Node loadRawYmlFromGit() {
        String path = getNodePathFromUrl();
        return ymlService.loadYmlContent(path, null);
    }

    /**
     * @api {post} /flows/:root/yml/stop Stop Load
     * @apiParam {String} root flow node name for stop yml loading
     * @apiGroup Flow Yml
     * @apiDescription Stop current yml loading threads,
     * and reset FLOW_YML_STATUS to NOF_FOUND if on loading status
     */
    @PostMapping("/{root}/yml/stop")
    @WebSecurity(action = Actions.FLOW_YML)
    public void stopLoadYml() {
        String path = getNodePathFromUrl();
        ymlService.stopLoadYmlContent(path);
    }

    /**
     * @api {post} /flows/:root/yml/verify Verify
     * @apiParam {String} root flow node name to verify yml
     * @apiParamExample {Yaml} Request-Body
     *  - flows:
     *      - name: xxx
     *      - steps:
     *          - name: xxx
     * @apiGroup Flow Yml
     *
     * @apiSuccessExample Success-Response
     *  HTTP/1.1 200 OK
     *
     * @apiErrorExample Error-Response
     *  HTTP/1.1 400 BAD REQUEST
     *
     *  {
     *      message: xxxx
     *  }
     */
    @PostMapping("/{root}/yml/verify")
    @WebSecurity(action = Actions.FLOW_YML)
    public void ymlVerification(@RequestBody String yml) {
        String path = getNodePathFromUrl();
        ymlService.verifyYml(path, yml);
    }

    /**
     * @api {post} /flows/:root/yml/create Create
     * @apiParam {String} root flow node name to set yml content
     * @apiParam Request-Body
     *  - flows:
     *      - name: xxx
     *      - steps:
     *          - name: xxx
     * @apiGroup Flow Yml
     * @apiDescription Create yml for flow,
     * the flow name must be matched with flow name defined in yml
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @PostMapping("/{root}/yml/create")
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Node createFromYml(@RequestBody String yml) {
        String path = getNodePathFromUrl();
        return nodeService.createOrUpdate(path, yml);
    }

    /**
     * @api {post} /flows/:root/flowAuthUsers
     * @apiParam {String} root flow node name
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"emailList" : ["test1@fir.im", "hl@fir.im"]
     *         	"flowPath": "flowPath"
     *     }
     * @apiGroup Flows
     *
     * @apiSuccessExample {list} Success-Response
     *  [
     *    {
     *      email: "xxxx",
     *      username: "xxxx",
     *      flows: [
     *        "aaa"
     *      ]
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *    },
     *    {}
     *  ]
     */
    @PostMapping("/{root}/flowAuthUsers")
    @WebSecurity(action = Actions.FLOW_AUTH)
    public List<User> flowAuthUsers(@RequestBody FlowAuthUser flowAuthUser){
        return nodeService.authUsers(flowAuthUser.getEmailList(), flowAuthUser.getFlowPath());
    }
}
