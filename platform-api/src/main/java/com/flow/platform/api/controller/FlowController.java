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
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.exception.NotImplementedException;
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
public class FlowController {

    @Autowired
    private NodeService nodeService;

    @GetMapping
    public List<Flow> index() {
        return nodeService.listFlows();
    }

    @GetMapping(path = "/{flowName}")
    public Flow show(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        String path = PathUtil.build(flowName);
        return (Flow) nodeService.findFlowInDb(path);
    }

    @PostMapping("/{flowName}")
    public Node createEmptyFlow(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        return nodeService.createEmptyFlow(flowName);
    }

    @PostMapping("/{flowName}/env")
    public void setFlowEnv(@PathVariable String flowName, @RequestBody Map<String, String> envs) {
        PathUtil.validateName(flowName);
        nodeService.setFlowEnv(PathUtil.build(flowName), envs);
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
        String yml = nodeService.getYmlContent(PathUtil.build(flowName));
        return yml == null ? "" : yml;
    }

    @GetMapping("/{flowName}/yml/load")
    public void loadRawYmlFromGit(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        nodeService.loadYmlContent(PathUtil.build(flowName), null);
    }

    @PostMapping("/{flowName}/yml/verify")
    public void ymlVerification(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        nodeService.verifyYml(PathUtil.build(flowName), yml);
    }

    @PostMapping("/{flowName}/yml/create")
    public Node createFromYml(@PathVariable String flowName, @RequestBody String yml) {
        PathUtil.validateName(flowName);
        return nodeService.create(PathUtil.build(flowName), yml);
    }

    @GetMapping("/{flowName}/webhook")
    public Webhook getWebhook(@PathVariable String flowName) {
        PathUtil.validateName(flowName);
        throw new NotImplementedException();
    }

    @GetMapping("/webhooks")
    public List<Webhook> listFlowWebhooks() {
        return nodeService.listWebhooks();
    }
}
