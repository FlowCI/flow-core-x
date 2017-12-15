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
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping(path = "/flows/{root}/steps")
public class FlowStepController extends NodeController {

    @Autowired
    private NodeService nodeService;

    /**
     * @api {get} /flows/:root/steps Get
     * @apiParam {String} root flow name
     * @apiGroup Flow Step
     * @apiDescription Get steps list
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          path: "flow/step",
     *          name: "step",
     *          envs: {
     *              FLOW_ENV_1: "var 1",
     *              FLOW_ENV_2: "var 2"
     *          }
     *          script: "echo xxx\nxx\n",
     *          conditionScript: "return true",
     *          plugin: "fir-cli",
     *          steps: []
     *          createdBy: "xxx@flow.ci",
     *          createdAt: "115123231",
     *          updatedAt: "151512323"
     *      },
     *
     *      ...
     *  ]
     */
    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Node> getChildren() {
        String root = currentNodePath.get();
        NodeTree tree = nodeService.find(root);
        return tree.children();
    }

    /**
     * @api {get} /flows/:root/steps Get
     * @apiParam {String} root flow name
     * @apiGroup Flow Step
     * @apiDescription Get steps list
     *
     * @apiParamExample {json} Request-Body
     *  [
     *      {
     *          name: "step",
     *          envs: {
     *              FLOW_ENV_1: "var 1",
     *              FLOW_ENV_2: "var 2"
     *          }
     *          script: "echo xxx\nxx\n",
     *          conditionScript: "return true",
     *          plugin: "fir-cli",
     *      },
     *
     *      ...
     *  ]
     */
    @PostMapping
    @WebSecurity(action = Actions.FLOW_CREATE)
    public void updateChildren(@RequestBody List<Node> children) {
        if (Objects.isNull(children) || children.isEmpty()) {
            throw new IllegalParameterException("The step list is required");
        }

        String root = currentNodePath.get();
        nodeService.updateByNodes(root, children);
    }
}
