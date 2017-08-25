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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.response.Existed;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.util.PathUtil;
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
    public List<Flow> index() {
        return nodeService.listFlows();
    }

    /**
     * @api {get} /flows/:flowname Show
     * @apiParam {String} flowname flow node name
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
    @GetMapping(path = "/{flowName}")
    public Node show(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        Node node = nodeService.find(PathUtil.build(flowName));
        if (node == null) {
            throw new IllegalParameterException(String.format("The flow name %s doesn't exist", flowName));
        }
        return node;
    }

    /**
     * @api {post} /flows/:flowname Create
     * @apiParam {String} flowname flow node name will be created
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
    @PostMapping("/{flowName}")
    public Node createEmptyFlow(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.createEmptyFlow(flowName);
    }

    /**
     * @api {post} /flows/:flowname/delete Delete
     * @apiParam {String} flowname flow node name will be deleted
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
    @PostMapping(path = "/{flowName}/delete")
    public Node delete(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.delete(PathUtil.build(flowName));
    }

    /**
     * @api {post} /flows/:flowname/env Set Env Variables
     * @apiParam {String} flowname flow node name will be set env variables
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
    @PostMapping("/{flowName}/env")
    public Node setFlowEnv(@PathVariable String flowName, @RequestBody Map<String, String> envs) {
        PathUtil.validateName(flowName);
        return nodeService.setFlowEnv(PathUtil.build(flowName), envs);
    }

    /**
     * @api {get} /flows/:flowname/exist IsExisted
     * @apiParam {String} flowname flow node name to check
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      existed: true
     *  }
     */
    @GetMapping("/{flowName}/exist")
    public Existed isFlowNameExist(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        boolean exist = nodeService.exist(PathUtil.build(flowName));
        return new Existed(exist);
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
    public List<Webhook> listFlowWebhooks() {
        return nodeService.listWebhooks();
    }

    /**
     * @api {get} /flows/:flowname/yml Get
     * @apiParam {String} flowname flow node name of yml
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
    @GetMapping(value = "/{flowName}/yml")
    public String getRawYml(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return ymlService.getYmlContent(PathUtil.build(flowName));
    }

    /**
     * @api {get} /flows/:flowname/yml/load Load
     * @apiParam {String} flowname flow node name to load yml
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
    @GetMapping("/{flowName}/yml/load")
    public Node loadRawYmlFromGit(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        String path = PathUtil.build(flowName);
        return ymlService.loadYmlContent(path, null);
    }

    /**
     * @api {get} /flows/:flowname/yml/stop Stop Load
     * @apiParam {String} flowname flow node name for stop yml loading
     * @apiGroup Flow Yml
     * @apiDescription Stop current yml loading threads,
     * and reset FLOW_YML_STATUS to NOF_FOUND if on loading status
     */
    @GetMapping("/{flowName}/yml/stop")
    public void stopLoadYml(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        ymlService.stopLoadYmlContent(PathUtil.build(flowName));
    }

    /**
     * @api {post} /flows/:flowname/yml/verify Verify
     * @apiParam {String} flowname flow node name to verify yml
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
    @PostMapping("/{flowName}/yml/verify")
    public void ymlVerification(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        ymlService.verifyYml(PathUtil.build(flowName), yml);
    }

    /**
     * @api {post} /flows/:flowname/yml/create Create
     * @apiParam {String} flowname flow node name to set yml content
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
    @PostMapping("/{flowName}/yml/create")
    public Node createFromYml(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        return nodeService.createOrUpdate(PathUtil.build(flowName), yml);
    }
}
