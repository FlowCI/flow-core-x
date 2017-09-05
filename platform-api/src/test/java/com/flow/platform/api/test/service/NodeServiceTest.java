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

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import java.nio.file.Files;
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

    @Autowired
    private YmlService ymlService;

    @Value(value = "${domain}")
    private String domain;

    @Test
    public void should_find_any_node() throws Throwable {
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);

        Assert.assertNotNull(nodeService.find(root.getPath()));

        List children = nodeService.find(root.getPath()).getChildren();
        Assert.assertNotNull(nodeService.find(((Node) children.get(0)).getPath()));
        Assert.assertNotNull(nodeService.find(((Node) children.get(1)).getPath()));
    }

    @Test
    public void should_create_node_by_yml() throws Throwable {
        // when: create empty flow and set special env for flow
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
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
        Assert.assertEquals("flow1", saved.getPath());

        // then: check root node can be loaded from node service
        root = nodeService.find(saved.getPath());
        Step step1 = (Step) root.getChildren().get(0);
        Assert.assertEquals("flow1/step1", step1.getPath());
        Step step2 = (Step) root.getChildren().get(1);
        Assert.assertEquals("flow1/step2", step2.getPath());

        // then: check env is merged from flow dao
        Assert.assertNotNull("111", root.getEnvs().get("FLOW_SP_1"));
        Assert.assertNotNull("222", root.getEnvs().get("FLOW_SP_2"));

        // then:
        Yml yaml = ymlDao.get(root.getPath());
        Assert.assertNotNull(yaml);
        Assert.assertEquals(resourceContent, yaml.getFile());
    }

    @Test
    public void should_yml_not_found_status_when_create_node_tree_from_empty_yml() throws Throwable {
        // given:
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

        Map<String, String> flowEnv = new HashMap<>();
        flowEnv.put("FLOW_YML_STATUS", "LOADING");
        nodeService.setFlowEnv(emptyFlow.getPath(), flowEnv);

        // when:
        nodeService.createOrUpdate(emptyFlow.getPath(), "");

        // then: check FLOW_YML_STATUS
        Node flow = nodeService.find(emptyFlow.getPath());
        Assert.assertEquals("NOT_FOUND", flow.getEnv("FLOW_YML_STATUS"));
    }

    @Test
    public void should_yml_error_status_when_create_node_tree_from_incorrect_yml() throws Throwable {
        // given:
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

        Map<String, String> flowEnv = new HashMap<>();
        flowEnv.put("FLOW_YML_STATUS", "LOADING");
        nodeService.setFlowEnv(emptyFlow.getPath(), flowEnv);

        // when:
        nodeService.createOrUpdate(emptyFlow.getPath(), "xxxx");

        // then: check FLOW_YML_STATUS
        Node flow = nodeService.find(emptyFlow.getPath());
        Assert.assertEquals("ERROR", flow.getEnv("FLOW_YML_STATUS"));
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
        Assert.assertEquals("PENDING", loaded.getEnv(FlowEnvs.FLOW_STATUS));
        Assert.assertEquals(webhook, loaded.getEnv(GitEnvs.FLOW_GIT_WEBHOOK));

        // should with empty yml
        Assert.assertNull(ymlDao.get(loaded.getPath()));

        // when:
        List<Webhook> hooks = nodeService.listWebhooks();
        Assert.assertEquals(1, hooks.size());
        Assert.assertEquals(domain + "/hooks/git/" + loaded.getName(), hooks.get(0).getHook());
    }

    @Test
    public void should_save_node_env() throws Throwable {
        // given:
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

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

        Node loaded = nodeService.find("flow1");
        Assert.assertEquals(8, loaded.getEnvs().size());
        Assert.assertEquals("hello", loaded.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", loaded.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", loaded.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", loaded.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", loaded.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, loaded.getEnv("FLOW_GIT_WEBHOOK"));

        Assert.assertEquals("READY", loaded.getEnv("FLOW_STATUS"));
        Assert.assertEquals("FOUND", loaded.getEnv("FLOW_YML_STATUS"));

        // check env been sync with yml
        Flow flow = flowDao.get("flow1");
        Assert.assertEquals(8, flow.getEnvs().size());
        Assert.assertEquals("hello", flow.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", flow.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", flow.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", flow.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", flow.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, flow.getEnv("FLOW_GIT_WEBHOOK"));

        Assert.assertEquals("READY", flow.getEnv("FLOW_STATUS"));
        Assert.assertEquals("FOUND", loaded.getEnv("FLOW_YML_STATUS"));
    }

    @Test(expected = YmlException.class)
    public void should_error_if_node_path_is_not_for_flow() throws Throwable {
        Flow emptyFlow = nodeService.createEmptyFlow("flow-name-not-same");
        setFlowToReady(emptyFlow);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);

        // then: FLOW_YML_STATUS should be ERROR
        Assert.assertEquals("ERROR", root.getEnv(FlowEnvs.FLOW_YML_STATUS));

        // then: should raise YmlParserException
        ymlService.getYmlContent(root.getPath());
    }

    @Test(expected = IllegalParameterException.class)
    public void should_delete_flow() throws Throwable {
        Flow emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdate(emptyFlow.getPath(), resourceContent);

        Assert.assertNotNull(nodeService.find(root.getPath()));
        Assert.assertNotNull(ymlService.getYmlContent(root.getPath()));

        // when:
        nodeService.delete(root.getPath());

        // then:
        Assert.assertNull(nodeService.find(root.getPath()));
        Assert.assertEquals(false, Files.exists(NodeUtil.workspacePath(workspace, root)));

        // then: should raise illegal parameter exception since flow doesn't exist
        ymlService.getYmlContent(root.getPath());
    }
}
