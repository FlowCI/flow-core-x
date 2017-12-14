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
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping(path = "/flows/{root}/steps")
public class StepController extends NodeController {

    @Autowired
    private NodeService nodeService;

    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Node> getChildren() {
        String root = currentNodePath.get();
        NodeTree tree = nodeService.find(root);
        return tree.children();
    }

    @PostMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public void updateChildren(Set<Node> children) {

    }
}
