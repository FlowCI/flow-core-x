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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.ListParam;
import com.flow.platform.api.domain.request.TriggerParam;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping(path = "/flows")
public class FlowController extends NodeController {

    @Autowired
    private YmlService ymlService;

    @Autowired
    private GitService gitService;

    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Node> index() {
        return nodeService.listFlows(true);
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
        String path = currentNodePath.get();
        Node node = nodeService.find(path).root();

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
        return nodeService.createEmptyFlow(currentNodePath.get());
    }

    /**
     * @api {delete} /flows/:root Delete
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
    @DeleteMapping(path = "/{root}")
    @WebSecurity(action = Actions.FLOW_DELETE)
    public Node delete() {
        return nodeService.delete(currentNodePath.get());
    }

    /**
     * @api {post} /flows/:root/env Add Env Variables
     * @apiParam {String} root flow node name will be set env variables
     * @apiParam {Boolean} [verify=false] enable to verify env varaible
     * @apiParamExample {json} Request-Body:
     *  {
     *      FLOW_ENV_VAR_2: xxx,
     *      FLOW_ENV_VAR_1: xxx
     *  }
     * @apiGroup Flows
     * @apiDescription Add env variables to flow env variables, overwrite if env existed
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
    public Node addFlowEnv(@RequestBody Map<String, String> envs,
                           @RequestParam(required = false, defaultValue = "false") boolean verify) {
        Node flow = nodeService.find(currentNodePath.get()).root();
        envService.save(flow, envs, verify);
        return flow;
    }

    /**
     * @api {delete} /flows/:root/env Del Env Variables
     * @apiParam {String} root flow node name will be set env variables
     * @apiParam {Boolean} [verify=false] enable to verify env varaible
     * @apiParamExample {json} Request-Body:
     *  [
     *      FLOW_ENV_VAR_2,
     *      FLOW_ENV_VAR_1
     *  ]
     * @apiGroup Flows
     * @apiDescription Delete env variables to flow env variables
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_3: xxx,
     *          FLOW_ENV_VAR_4: xxx
     *      }
     *  }
     */
    @DeleteMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Node delFlowEnv(@RequestBody Set<String> envKeys,
                           @RequestParam(required = false, defaultValue = "false") boolean verify) {
        Node flow = nodeService.find(currentNodePath.get()).root();
        envService.delete(flow, envKeys, verify);
        return flow;
    }

    /**
     * @api {get} /flows/:root/env Get Env
     * @apiParam {String} root root node name
     * @apiParam {String} [key] env variable name, ex: http://xxxx/flows/xx/env?key=FLOW_GIT_WEBHOOK
     * @apiParam {Boolean} [editable = true] is get editalbe only variable
     * @apiGroup Flows
     * @apiDescription Get node env by path or name
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      FLOW_ENV_VAR: xxx
     *  }
     */
    @GetMapping(path = "/{root}/env")
    @WebSecurity(action = Actions.FLOW_SHOW)
    public Map<String, String> getFlowEnv(@RequestParam(required = false) String key,
                                          @RequestParam(required = false) BooleanValue editable) {

        Node flow = nodeService.find(currentNodePath.get()).root();

        // return all env variables
        if (editable == null) {
            return flow.getEnvs();
        }

        Map<String, String> envs = envService.list(flow, editable.getValue());
        if (Strings.isNullOrEmpty(key)) {
            return envs;
        }

        return EnvUtil.build(key, envs.get(key));
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
        boolean exist = nodeService.exist(currentNodePath.get());
        return new BooleanValue(exist);
    }

    /**
     * @api {get} /flows/:root/branches List Branches
     * @apiParam {String} root flow node name
     * @apiParam {Boolean} [refresh] true or false, the default is false
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  [
     *      master,
     *      develop,
     *      feature/xxx/xxx
     *  ]
     */
    @GetMapping("/{root}/branches")
    public List<String> listBranches(@RequestParam(required = false) Boolean refresh) {
        if (refresh == null) {
            refresh = false;
        }

        Node root = nodeService.find(currentNodePath.get()).root();
        return gitService.branches(root, refresh);
    }

    /**
     * @api {get} /flows/:root/tags List Tags
     * @apiParam {String} root flow node name
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  [
     *      v1.0,
     *      v2.0
     *  ]
     */
    @GetMapping("/{root}/tags")
    public List<String> listTags() {
        Node root = nodeService.find(currentNodePath.get()).root();
        return gitService.tags(root, false);
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
        Node root = nodeService.find(currentNodePath.get()).root();
        return ymlService.get(root).getFile();
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
        Node root = nodeService.find(currentNodePath.get()).root();
        return ymlService.loadYmlContent(root, null, null);
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
        Node root = nodeService.find(currentNodePath.get()).root();
        ymlService.stopLoadYmlContent(root);
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
        Node root = nodeService.find(currentNodePath.get()).root();
        ymlService.verifyYml(root, yml);
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
     *
     *  yml body
     */
    @PostMapping("/{root}/yml")
    @WebSecurity(action = Actions.FLOW_CREATE)
    public String createFromYml(@RequestBody String yml) {
        nodeService.createOrUpdateYml(currentNodePath.get(), yml);
        return yml;
    }

    /**
     * @api {post} /flows/:root/users/auth
     * @apiParam {String} root flow node name
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"arrays" : ["test1@fir.im", "hl@fir.im"]
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
    @PostMapping("/{root}/users/auth")
    @WebSecurity(action = Actions.FLOW_AUTH)
    public List<User> flowAuthUsers(@RequestBody ListParam<String> listParam) {
        return nodeService.authUsers(listParam.getArrays(), currentNodePath.get());
    }

    /**
     * @api {post} /flows/:root/trigger
     * @apiParam {String} root
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"branchFilter" : ["master", "dev"]
     *         	"tagFilter" : ["v01", "v02"]
     *         	"tagEnabled": true
     *         	"pushEnabled": false
     *         	"prEnabled": true
     *     }
     * @apiGroup Flows
     *
     * @apiSuccessExample {list} Success-Response
     *  {
     *      "branchFilter": [
     *          "master",
     *          "develop"
     *     ],
     *     "tagFilter": [
     *          "aa"
     *     ]
     *     "tagEnable": false,
     *     "pushEnable": true,
     *     "prEnable": false,
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      branchFilter: []
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     */
    @PostMapping("/{root}/trigger")
    public Node trigger(@RequestBody TriggerParam triggerParam){
        String path = currentNodePath.get();
        Node flow = nodeService.find(path).root();
        envService.save(flow, triggerParam.toEnv(), true);
        return flow;
    }
}
