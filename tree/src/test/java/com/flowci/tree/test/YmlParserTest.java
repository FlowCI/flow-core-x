/*
 * Copyright 2018 flow.ci
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

package com.flowci.tree.test;

import com.flowci.domain.DockerOption;
import com.flowci.exception.YmlException;
import com.flowci.tree.*;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.flowci.tree.FlowNode.DEFAULT_ROOT_NAME;

/**
 * @author yang
 */
public class YmlParserTest {

    private String content;

    @Before
    public void init() throws IOException {
        content = loadContent("flow.yml");
    }

    @Test(expected = YmlException.class)
    public void should_yml_exception_if_name_is_invalid() throws IOException {
        content = loadContent("flow-with-invalid-name.yml");
        YmlParser.load(content);
    }

    @Test
    public void should_get_node_from_yml() {
        FlowNode root = YmlParser.load(content);

        // verify flow
        Assert.assertEquals(DEFAULT_ROOT_NAME, root.getName());
        Assert.assertEquals("echo hello", root.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", root.getEnv("FLOW_VERSION"));

        Assert.assertTrue(root.getSelector().getLabel().contains("ios"));
        Assert.assertTrue(root.getSelector().getLabel().contains("local"));

        Assert.assertNotNull(root.getCondition());

        // verify docker
        Assert.assertTrue(root.getDockers().size() > 0);
        Assert.assertEquals("helloworld:0.1", root.getDockers().get(0).getImage());

        // verify notifications
        Assert.assertEquals(1, root.getNotifications().size());
        Assert.assertEquals("email-notify", root.getNotifications().get(0).getPlugin());
        Assert.assertEquals("test-config", root.getNotifications().get(0).getEnvs().get("FLOWCI_SMTP_CONFIG"));

        // verify steps
        List<StepNode> steps = root.getChildren();
        Assert.assertEquals(2, steps.size());

        RegularStepNode step1 = (RegularStepNode) steps.get(0);
        Assert.assertEquals("step-1", step1.getName()); // step-1 is default name
        Assert.assertEquals("echo step", step1.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo step version", step1.getEnv("FLOW_VERSION"));
        Assert.assertEquals(3600, step1.getTimeout().intValue());

        Assert.assertTrue(step1.isAllowFailure());
        Assert.assertEquals("println(FLOW_WORKSPACE)\ntrue\n", step1.getCondition());

        // verify cache
        Assert.assertNotNull(step1.getCache());
        Assert.assertEquals(2, step1.getCache().getPaths().size());
        Assert.assertEquals("mycache", step1.getCache().getKey());
        Assert.assertEquals("./", step1.getCache().getPaths().get(0));
        Assert.assertEquals("vendor", step1.getCache().getPaths().get(1));

        RegularStepNode step2 = (RegularStepNode) steps.get(1);
        Assert.assertEquals("step2", step2.getName());
        Assert.assertEquals("echo 2", step2.getBash());
        Assert.assertEquals("echo powershell", step2.getPwsh());
        Assert.assertNull(step2.getTimeout());

        DockerOption dockerOption = step2.getDockers().get(0);
        Assert.assertNotNull(dockerOption);
        Assert.assertEquals("ubuntu:18.04", dockerOption.getImage());
        Assert.assertEquals("6400:6400", dockerOption.getPorts().get(0));
        Assert.assertEquals("2700:2700", dockerOption.getPorts().get(1));
        Assert.assertEquals("/bin/sh", dockerOption.getEntrypoint().get(0));
        Assert.assertEquals("host", dockerOption.getNetwork());
    }

    @Test
    public void should_get_correct_relationship_on_node_tree() {
        FlowNode root = YmlParser.load(content);
        NodeTree tree = NodeTree.create(root);
        Assert.assertEquals(root, tree.getRoot());

        // verify parent / child relationship
        RegularStepNode step1 = (RegularStepNode) tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step-1")); // step-1 is default name
        Assert.assertNotNull(step1);
        Assert.assertEquals(root, step1.getParent());

        RegularStepNode step2 = (RegularStepNode) tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step2"));
        Assert.assertNotNull(step2);
        Assert.assertTrue(step2.getChildren().isEmpty());
        Assert.assertEquals(root, step2.getParent());
        Assert.assertEquals(step2, tree.next(step1.getPath()));
    }

//    @Test
//    public void should_parse_yml_with_exports_filter() throws IOException {
//        content = loadContent("flow-with-exports.yml");
//        FlowNode root = YmlParser.load(content);
//        NodeTree tree = NodeTree.create(root);
//
//        RegularStepNode first = (RegularStepNode) tree.next(tree.getRoot().getPath());
//        Assert.assertEquals("step-1", first.getPath().name());
//
//        Assert.assertEquals(2, first.getExports().size());
//    }

//    @Test
//    public void should_parse_docker_and_dockers() throws IOException {
//        content = loadContent("step-with-dockers.yml");
//        FlowNode root = YmlParser.load(content);
//        NodeTree tree = NodeTree.create(root);
//
//        RegularStepNode first = (RegularStepNode) tree.getSteps().get(0);
//        Assert.assertEquals(1, first.getDockers().size());
//        Assert.assertEquals("ubuntu:18.04", first.getDockers().get(0).getImage());
//        Assert.assertTrue(first.getDockers().get(0).isRuntime());
//
//        RegularStepNode second = (RegularStepNode) tree.getSteps().get(1);
//        Assert.assertEquals(2, second.getDockers().size());
//
//        Assert.assertEquals("ubuntu:18.04", second.getDockers().get(0).getImage());
//        Assert.assertTrue(second.getDockers().get(0).isRuntime());
//
//        Assert.assertEquals("mysql", second.getDockers().get(1).getImage());
//        Assert.assertEquals("12345", second.getDockers().get(1).getEnvironment().get("MY_PW"));
//
//        Assert.assertEquals(2, second.getDockers().get(1).getCommand().size());
//        Assert.assertEquals("mysql", second.getDockers().get(1).getCommand().get(0));
//        Assert.assertEquals("-hlocalhost", second.getDockers().get(1).getCommand().get(1));
//
//        Assert.assertFalse(second.getDockers().get(1).isRuntime());
//    }

    @Test(expected = YmlException.class)
    public void should_throw_ex_when_runtime_has_command() throws IOException {
        content = loadContent("runtime-with-command.yml");
        YmlParser.load(content);
    }

//    @Test
//    public void should_parse_step_in_step() throws IOException {
//        content = loadContent("step-in-step.yml");
//
//        FlowNode root = YmlParser.load(content);
//        Assert.assertEquals(3, root.getChildren().size());
//
//        RegularStepNode step2 = (RegularStepNode) root.getChildren().get(1);
//        Assert.assertEquals(root, step2.getParent());
//
//        Assert.assertEquals(2, step2.getChildren().size());
//        RegularStepNode step2_1 = (RegularStepNode) step2.getChildren().get(0);
//        Assert.assertEquals(step2, step2_1.getParent());
//
//        RegularStepNode step2_2 = (RegularStepNode) step2.getChildren().get(1);
//        Assert.assertEquals(step2, step2_2.getParent());
//
//        NodeTree tree = NodeTree.create(root);
//        Assert.assertEquals(5, tree.getSteps().size());
//        Assert.assertEquals("step2", tree.nextRootStep(NodePath.create(DEFAULT_ROOT_NAME, "step-1")).getName());
//        Assert.assertEquals("create test", tree.nextRootStep(NodePath.create(DEFAULT_ROOT_NAME, "step2")).getName());
//
//        RegularStepNode step3 = (RegularStepNode) tree.nextRootStep(NodePath.create(DEFAULT_ROOT_NAME, "step2", "step-2-1"));
//        Assert.assertNotNull(step3);
//        Assert.assertEquals("create test", step3.getName());
//
//        Assert.assertNull(tree.nextRootStep(step3.getPath()));
//    }

    @Test(expected = YmlException.class)
    public void should_throw_ex_when_plugin_defined_in_parent_step() throws IOException {
        content = loadContent("parent-step-with-plugin.yml");
        YmlParser.load(content);
    }

    @Test
    public void should_load_parallel_step_yaml() throws IOException {
        content = loadContent("flow-parallel.yml");
        FlowNode root = YmlParser.load(content);
        Assert.assertNotNull(root);

        NodeTree tree = NodeTree.create(root);
        Assert.assertNotNull(tree);
        Assert.assertEquals(root, tree.getRoot());

        Assert.assertEquals(9, tree.numOfNode());
        Assert.assertEquals(8, tree.getMaxOrder());

        // validate get next of root
        List<StepNode> parallelStep = tree.next(root.getPath());
        Assert.assertEquals(1, parallelStep.size());
        NodePath expected = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1");
        Assert.assertEquals(expected, parallelStep.get(0).getPath());

        // next should be first two nodes in parallel flow
        List<StepNode> firstNodesOfSubflow = tree.next(expected);
        Assert.assertEquals(2, firstNodesOfSubflow.size());

        StepNode subflowA_A = firstNodesOfSubflow.get(0);
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-A", "A"), subflowA_A.getPath());

        StepNode subflowB_A = firstNodesOfSubflow.get(1);
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-B", "A"), subflowB_A.getPath());

        // subflow-A/A next should be the second node of subflow-A/B
        List<StepNode> subflowA_B = tree.next(subflowA_A.getPath());
        Assert.assertEquals(1, subflowA_B.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-A", "B"), subflowA_B.get(0).getPath());

        // subflow-A/B next should be step 2
        List<StepNode> step2 = tree.next(subflowA_B.get(0).getPath());
        Assert.assertEquals(1, step2.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step2"), step2.get(0).getPath());

        // subflow-B/A next should be step2
        step2 = tree.next(subflowB_A.getPath());
        Assert.assertEquals(1, step2.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step2"), step2.get(0).getPath());

        // step2 next should be step3
        List<StepNode> step3 = tree.next(step2.get(0).getPath());
        Assert.assertEquals(1, step3.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step3"), step3.get(0).getPath());

        // step3 next should be step-3-1
        List<StepNode> step3_1 = tree.next(step3.get(0).getPath());
        Assert.assertEquals(1, step3_1.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step3", "step-3-1"), step3_1.get(0).getPath());

        // step-3-1 next should be step-3-2
        List<StepNode> step3_2 = tree.next(step3_1.get(0).getPath());
        Assert.assertEquals(1, step3_2.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step3", "step-3-2"), step3_2.get(0).getPath());

        // step-3-2 next should be step4
        List<StepNode> step4 = tree.next(step3_2.get(0).getPath());
        Assert.assertEquals(1, step4.size());
        Assert.assertEquals(NodePath.create(DEFAULT_ROOT_NAME, "step4"), step4.get(0).getPath());
    }

    private String loadContent(String resource) throws IOException {
        ClassLoader classLoader = YmlParserTest.class.getClassLoader();
        URL url = classLoader.getResource(resource);
        return Files.toString(new File(url.getFile()), StandardCharsets.UTF_8);
    }
}
