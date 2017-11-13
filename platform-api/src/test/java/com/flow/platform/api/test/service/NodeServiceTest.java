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

import static junit.framework.TestCase.fail;

import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.FlowEnvs.StatusValue;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.envs.GitToggleEnvs;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.http.HttpURL;
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
    private YmlService ymlService;

    @Value(value = "${domain.api}")
    private String apiDomain;

    @Test
    public void should_find_any_node() throws Throwable {
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdateYml(emptyFlow.getPath(), resourceContent);

        Assert.assertNotNull(nodeService.find(root.getPath()));

        List children = nodeService.find(root.getPath()).root().getChildren();
        Assert.assertNotNull(nodeService.find(((Node) children.get(0)).getPath()));
        Assert.assertNotNull(nodeService.find(((Node) children.get(1)).getPath()));
    }

    @Test
    public void should_create_node_by_yml() throws Throwable {
        // when: create empty flow and set special env for flow
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
        Map<String, String> flowEnv = new HashMap<>();
        flowEnv.put("FLOW_SP_1", "111");
        flowEnv.put("FLOW_SP_2", "222");
        envService.save(emptyFlow, flowEnv, false);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdateYml(emptyFlow.getPath(), resourceContent);

        // then: check is created in dao
        Node saved = flowDao.get(root.getPath());
        Assert.assertNotNull(saved);
        Assert.assertTrue(nodeService.exist(root.getPath()));
        Assert.assertEquals(root, saved);
        Assert.assertEquals("flow1", saved.getName());
        Assert.assertEquals("flow1", saved.getPath());

        // then: check root node can be loaded from node service
        root = nodeService.find(saved.getPath()).root();
        Node step1 = root.getChildren().get(0);
        Assert.assertEquals("flow1/step1", step1.getPath());
        Node step2 = root.getChildren().get(1);
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
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
        envService.save(emptyFlow, EnvUtil.build("FLOW_YML_STATUS", "LOADING"), false);

        // when:
        try {
            nodeService.createOrUpdateYml(emptyFlow.getPath(), "");
            fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof YmlException);
        }

        // then: check FLOW_YML_STATUS
        Node flow = nodeService.find(emptyFlow.getPath()).root();
        Assert.assertEquals("NOT_FOUND", flow.getEnv("FLOW_YML_STATUS"));
    }

    @Test
    public void should_yml_error_status_when_create_node_tree_from_incorrect_yml() throws Throwable {
        // given:
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);
        envService.save(emptyFlow, EnvUtil.build("FLOW_YML_STATUS", "LOADING"), false);

        // when:
        try {
            nodeService.createOrUpdateYml(emptyFlow.getPath(), "xxxx");
            fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof YmlException);
        }

        // then: check FLOW_YML_STATUS
        Node flow = nodeService.find(emptyFlow.getPath()).root();
        Assert.assertEquals("ERROR", flow.getEnv("FLOW_YML_STATUS"));
    }

    @Test
    public void should_create_empty_flow_and_get_webhooks() {
        // when:
        String flowName = "default";
        Node emptyFlow = nodeService.createEmptyFlow(flowName);

        // then:
        Node loaded = nodeService.find(emptyFlow.getPath()).root();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(emptyFlow, loaded);
        Assert.assertEquals(0, loaded.getChildren().size());

        String webhook = HttpURL.build(apiDomain).append("/hooks/git").append(loaded.getName()).toString();
        Assert.assertEquals("PENDING", loaded.getEnv(FlowEnvs.FLOW_STATUS));
        Assert.assertEquals(webhook, loaded.getEnv(GitEnvs.FLOW_GIT_WEBHOOK));

        // should with empty yml
        Assert.assertNull(ymlDao.get(loaded.getPath()));

        // when:
        List<Webhook> hooks = nodeService.listWebhooks();
        Assert.assertEquals(1, hooks.size());
        Assert.assertEquals(apiDomain + "/hooks/git/" + loaded.getName(), hooks.get(0).getHook());
    }

    @Test
    public void should_save_node_env() throws Throwable {
        // given:
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdateYml(emptyFlow.getPath(), resourceContent);
        Assert.assertEquals("echo hello", root.getEnvs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", root.getEnvs().get("FLOW_VERSION"));

        // save illegal env variable
        root.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT, "1111");
        root.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH, "master");
        flowDao.update(root);

        // when: save other env variable without check illegal env just saved
        Map<String, String> envs = new HashMap<>(3);
        envs.put("FLOW_NEW_1", "hello");
        envs.put("FLOW_NEW_2", "world");
        envs.put("FLOW_NEW_3", "done");
        envService.save(nodeService.find("flow1").root(), envs, true);

        // then:
        String webhook = HttpURL.build(apiDomain).append("/hooks/git").append(emptyFlow.getName()).toString();
        Node loaded = nodeService.find("flow1").root();
        Assert.assertEquals(15, loaded.getEnvs().size());
        Assert.assertEquals("hello", loaded.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", loaded.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", loaded.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", loaded.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", loaded.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, loaded.getEnv("FLOW_GIT_WEBHOOK"));

        Assert.assertEquals("READY", loaded.getEnv("FLOW_STATUS"));
        Assert.assertEquals("FOUND", loaded.getEnv("FLOW_YML_STATUS"));

        Assert.assertEquals("true", loaded.getEnv(GitToggleEnvs.FLOW_GIT_PUSH_ENABLED));
        Assert.assertEquals("true", loaded.getEnv(GitToggleEnvs.FLOW_GIT_TAG_ENABLED));
        Assert.assertEquals("true", loaded.getEnv(GitToggleEnvs.FLOW_GIT_PR_ENABLED));
        Assert.assertEquals("[\"*\"]", loaded.getEnv(GitToggleEnvs.FLOW_GIT_PUSH_FILTER));
        Assert.assertEquals("[\"*\"]", loaded.getEnv(GitToggleEnvs.FLOW_GIT_TAG_FILTER));

        // check env been sync with yml
        Node flow = flowDao.get("flow1");
        Assert.assertEquals(15, flow.getEnvs().size());
        Assert.assertEquals("hello", flow.getEnv("FLOW_NEW_1"));
        Assert.assertEquals("world", flow.getEnv("FLOW_NEW_2"));
        Assert.assertEquals("done", flow.getEnv("FLOW_NEW_3"));
        Assert.assertEquals("echo hello", flow.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", flow.getEnv("FLOW_VERSION"));
        Assert.assertEquals(webhook, flow.getEnv("FLOW_GIT_WEBHOOK"));

        Assert.assertEquals("READY", flow.getEnv("FLOW_STATUS"));
        Assert.assertEquals("FOUND", loaded.getEnv("FLOW_YML_STATUS"));
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_when_save_flow_env_since_illegal_format() throws Throwable {
        // given:
        Node flow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(flow);
        Assert.assertEquals(StatusValue.READY.value(), flow.getEnv(FlowEnvs.FLOW_STATUS));

        // when:
        Map<String, String> envs = new HashMap<>();
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH.name(), "master");
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT.name(), "illegal crontab format");

        envService.save(nodeService.find("flow1").root(), envs, false);
    }

    @Test
    public void should_error_if_node_path_is_not_for_flow() throws Throwable {
        Node emptyFlow = nodeService.createEmptyFlow("flow_name_not_same");
        setFlowToReady(emptyFlow);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdateYml(emptyFlow.getPath(), resourceContent);

        Assert.assertEquals("FOUND", root.getEnv(FlowEnvs.FLOW_YML_STATUS));

        // then: should raise YmlParseException
        ymlService.get(root);
    }

    @Test
    public void should_delete_flow() throws Throwable {
        Node emptyFlow = nodeService.createEmptyFlow("flow1");
        setFlowToReady(emptyFlow);

        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.createOrUpdateYml(emptyFlow.getPath(), resourceContent);

        Assert.assertNotNull(nodeService.find(root.getPath()));
        Assert.assertNotNull(ymlService.get(root).getFile());

        // when: delete flow
        nodeService.delete(root.getPath());

        // then: flow should be null
        Assert.assertNull(nodeService.find(root.getPath()));
        Assert.assertEquals(false, Files.exists(NodeUtil.workspacePath(workspace, root)));

        // then: should return null if root node is not existed
        Assert.assertNull(ymlService.get(root));
    }
}
