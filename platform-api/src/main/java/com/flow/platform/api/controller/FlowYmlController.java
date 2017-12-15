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
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.util.StringUtil;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping(path = "/flows/{root}/yml")
public class FlowYmlController extends NodeController {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private YmlService ymlService;

    /**
     * @api {get} /flows/:root/yml Get
     * @apiParam {String} root flow node name of yml
     * @apiGroup Flow YML
     * @apiDescription Get flow node related yml content,
     * response empty yml content if it is loading from git repo
     *
     * @apiSuccessExample {yaml} Success-Response
     *  - flows
     *      - name: xx
     *      - steps:
     *          - name: xxx
     */
    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public String getYml() {
        Node root = nodeService.find(currentNodePath.get()).root();
        Yml yml = ymlService.get(root);

        if (yml != null) {
            return yml.getFile();
        }

        return StringUtil.EMPTY;
    }

    /**
     * @api {post} /flows/:root/yml/download YML Download
     * @apiParam {String} root flow node name to set yml content
     * @apiGroup Flow YML
     * @apiDescription download yml for flow,
     * the flow name must be matched with flow name defined in yml
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  yml file
     */
    @GetMapping("/download")
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Resource downloadFlowYml(HttpServletResponse httpResponse) {
        String path = currentNodePath.get();
        Node root = nodeService.find(path).root();
        httpResponse.setHeader(
            "Content-Disposition",
            String.format("attachment; filename=%s", root.getName() + ".yml"));
        return ymlService.getResource(root);
    }

    /**
     * @api {post} /flows/:root/yml YML Update
     * @apiParam {String} root flow node name to update yml content
     * @apiParam Request-Body
     *  - flows:
     *      - name: xxx
     *      - steps:
     *          - name: xxx
     *
     * @apiGroup Flow YML
     * @apiDescription Update yml for flow,
     * the flow name must be matched with flow name defined in yml
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  yml body
     */
    @PostMapping
    @WebSecurity(action = Actions.FLOW_CREATE)
    public String updateYml(@RequestBody String yml) {
        nodeService.updateByYml(currentNodePath.get(), yml);
        return yml;
    }

    /**
     * @api {get} /flows/:root/yml/load Load
     * @apiParam {String} root flow node name to load yml
     * @apiGroup Flow YML
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
    @GetMapping("/load")
    @WebSecurity(action = Actions.FLOW_YML)
    public Node loadRawYmlFromGit() {
        Node root = nodeService.find(currentNodePath.get()).root();
        return ymlService.startLoad(root, null, null);
    }

    /**
     * @api {post} /flows/:root/yml/stop Stop Load
     * @apiParam {String} root flow node name for stop yml loading
     * @apiGroup Flow YML
     * @apiDescription Stop current yml loading threads,
     * and reset FLOW_YML_STATUS to NOF_FOUND if on loading status
     */
    @PostMapping("/stop")
    @WebSecurity(action = Actions.FLOW_YML)
    public void stopLoadYml() {
        Node root = nodeService.find(currentNodePath.get()).root();
        ymlService.stopLoad(root);
    }

    /**
     * @api {post} /flows/:root/yml/verify YML Verify
     * @apiParam {String} root flow node name to verify yml
     * @apiParamExample {yaml} Request-Body
     *  - flows:
     *      - name: xxx
     *      - steps:
     *          - name: xxx
     * @apiGroup Flow YML
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
    @PostMapping("/verify")
    @WebSecurity(action = Actions.FLOW_YML)
    public void ymlVerification(@RequestBody String yml) {
        Node root = nodeService.find(currentNodePath.get()).root();
        ymlService.build(root, yml);
    }
}
