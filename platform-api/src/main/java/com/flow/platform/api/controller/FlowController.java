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

    @GetMapping(path = "/{flowName}")
    public Node show(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        Node node = nodeService.find(PathUtil.build(flowName));
        if (node == null) {
            throw new IllegalParameterException(String.format("The flow name %s doesn't exist", flowName));
        }
        return node;
    }

    @PostMapping("/{flowName}")
    public Node createEmptyFlow(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.createEmptyFlow(flowName);
    }

    @PostMapping(path = "/{flowName}/delete")
    public Node delete(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.delete(PathUtil.build(flowName));
    }

    @PostMapping("/{flowName}/env")
    public Node setFlowEnv(@PathVariable String flowName, @RequestBody Map<String, String> envs) {
        PathUtil.validateName(flowName);
        return nodeService.setFlowEnv(PathUtil.build(flowName), envs);
    }

    /**
     * Check flow name is exist
     */
    @GetMapping("/{flowName}/exist")
    public Boolean isFlowNameExist(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.exist(PathUtil.build(flowName));
    }

    @GetMapping("/{flowName}/yml")
    public String getRawYml(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return ymlService.getYmlContent(PathUtil.build(flowName));
    }

    @GetMapping("/{flowName}/yml/load")
    public Node loadRawYmlFromGit(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        String path = PathUtil.build(flowName);
        return ymlService.loadYmlContent(path, null);
    }

    @GetMapping("/{flowName}/yml/stop")
    public void stopLoadYml(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        ymlService.stopLoadYmlContent(PathUtil.build(flowName));
    }

    @PostMapping("/{flowName}/yml/verify")
    public void ymlVerification(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        ymlService.verifyYml(PathUtil.build(flowName), yml);
    }

    @PostMapping("/{flowName}/yml/create")
    public Node createFromYml(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        return nodeService.createOrUpdate(PathUtil.build(flowName), yml);
    }

    @GetMapping("/webhooks")
    public List<Webhook> listFlowWebhooks() {
        return nodeService.listWebhooks();
    }
}
