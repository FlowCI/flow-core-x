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
package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yh@firim
 */
public class NodeServiceTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Value(value = "${domain}")
    private String domain;

    @Test
    public void should_find_any_node() throws Throwable {
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);

        Assert.assertNotNull(nodeService.find(root.getPath()));

        List children = root.getChildren();
        Assert.assertNotNull(nodeService.find(((Node) children.get(0)).getPath()));
        Assert.assertNotNull(nodeService.find(((Node) children.get(1)).getPath()));
    }

    @Test
    public void should_create_node_by_yml() throws Throwable {
        // when: create empty flow and set special env for flow
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        Map<String, String> flowEnv = new HashMap<>();
        flowEnv.put("FLOW_SP_1", "111");
        flowEnv.put("FLOW_SP_2", "222");
        nodeService.setFlowEnv(emptyFlow.getPath(), flowEnv);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);


        // then: check is created in dao
        Flow saved = flowDao.get(root.getPath());
        Assert.assertNotNull(saved);
        Assert.assertTrue(nodeService.exist(root.getPath()));
        Assert.assertEquals(root, saved);
        Assert.assertEquals("flow1", saved.getName());
        Assert.assertEquals("/flow1", saved.getPath());

        // then: check root node can be loaded from node service
        root = nodeService.find(saved.getPath());
        Step step1 = (Step) root.getChildren().get(0);
        Assert.assertEquals("/flow1/step1", step1.getPath());
        Step step2 = (Step) root.getChildren().get(1);
        Assert.assertEquals("/flow1/step2", step2.getPath());

        // then: check env is merged from flow dao
        Assert.assertNotNull("111", root.getEnvs().get("FLOW_SP_1"));
        Assert.assertNotNull("222", root.getEnvs().get("FLOW_SP_2"));

        // then:
        YmlStorage yaml = ymlStorageDao.get(root.getPath());
        Assert.assertNotNull(yaml);
        Assert.assertEquals(resourceContent, yaml.getFile());
    }

    @Test
    public void should_create_empty_flow_and_get_webhooks() {
        // when:
        String flowName = "default";
        Flow emptyFlow = nodeService.createEmptyFlow(flowName);

        // then:
        Flow loaded = (Flow) nodeService.find(emptyFlow.getPath());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(emptyFlow, loaded);
        Assert.assertEquals(0, loaded.getChildren().size());

        String webhook = String.format("%s/hooks/git/%s", domain, loaded.getName());
        Assert.assertEquals(FlowEnvs.Value.FLOW_STATUS_PENDING.value(), loaded.getEnv(FlowEnvs.FLOW_STATUS));
        Assert.assertEquals(webhook, loaded.getEnv(GitEnvs.FLOW_GIT_WEBHOOK));

        // should with empty yml
        Assert.assertNull(ymlStorageDao.get(loaded.getPath()));

        // when:
        List<Webhook> hooks = nodeService.listWebhooks();
        Assert.assertEquals(1, hooks.size());
        Assert.assertEquals(domain + "/hooks/git/" + loaded.getName(), hooks.get(0).getHook());
    }

    @Test
    public void should_save_node_env() throws Throwable {
        // given:
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);
        Assert.assertEquals("echo hello", root.getEnvs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", root.getEnvs().get("FLOW_VERSION"));

        // when:
        Map<String, String> envs = new HashMap<>();
        envs.put("FLOW_NEW_1", "hello");
        envs.put("FLOW_NEW_2", "world");
        envs.put("FLOW_NEW_3", "done");
        nodeService.setFlowEnv(root.getPath(), envs);

        // then:
        String webhook = String.format("%s/hooks/git/%s", domain, emptyFlow.getName());

        Node loaded = nodeService.find("/flow1");
        Assert.assertEquals(7, loaded.getEnvs().size());
        Assert.assertEquals("hello", loaded.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", loaded.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", loaded.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", loaded.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", loaded.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, loaded.getEnv("FLOW_GIT_WEBHOOK"));
        Assert.assertEquals("PENDING", loaded.getEnv("FLOW_STATUS"));

        // check env been sync with yml
        Flow flow = flowDao.get("/flow1");
        Assert.assertEquals(7, flow.getEnvs().size());
        Assert.assertEquals("hello", flow.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", flow.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", flow.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", flow.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", flow.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, flow.getEnv("FLOW_GIT_WEBHOOK"));
        Assert.assertEquals("PENDING", flow.getEnv("FLOW_STATUS"));
    }

    @Test(expected = IllegalParameterException.class)
    public void should_error_if_node_path_is_not_for_flow() throws Throwable {
        // given:
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);

        // when: set env with child path
        List children = root.getChildren();
        nodeService.setFlowEnv(((Node) children.get(0)).getPath(), new HashMap<>());
    }
}
