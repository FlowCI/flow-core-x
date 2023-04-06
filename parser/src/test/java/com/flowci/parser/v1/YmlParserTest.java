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

package com.flowci.parser.v1;

import com.flowci.domain.tree.DockerOption;
import com.flowci.exception.YmlException;
import com.flowci.domain.tree.NodePath;
import com.flowci.parser.TestUtil;
import com.google.common.io.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.flowci.parser.v1.FlowNode.DEFAULT_ROOT_NAME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class YmlParserTest {

    private String content;

    @BeforeEach
    public void init() throws IOException {
        content = TestUtil.loadContent("v1/flow.yml");
    }

    @Test
    public void should_yml_exception_if_name_is_invalid() throws IOException {
        content = TestUtil.loadContent("v1/flow-with-invalid-name.yml");
        Assertions.assertThrows(YmlException.class, () -> YmlParser.load(content));
    }

    @Test
    public void should_get_node_from_yml() {
        FlowNode root = YmlParser.load(content);

        // verify flow
        assertEquals(DEFAULT_ROOT_NAME, root.getName());
        assertEquals("echo hello", root.getEnv("FLOW_WORKSPACE"));
        assertEquals("echo version", root.getEnv("FLOW_VERSION"));

        assertTrue(root.getSelector().getLabel().contains("ios"));
        assertTrue(root.getSelector().getLabel().contains("local"));

        assertNotNull(root.getCondition());

        // verify docker
        assertTrue(root.getDockers().size() > 0);
        assertEquals("helloworld:0.1", root.getDockers().get(0).getImage());

        // verify steps
        List<Node> steps = root.getChildren();
        assertEquals(2, steps.size());

        RegularStepNode step1 = (RegularStepNode) steps.get(0);
        assertEquals("step-1", step1.getName()); // step-1 is default name
        assertEquals("echo step", step1.getEnv("FLOW_WORKSPACE"));
        assertEquals("echo step version", step1.getEnv("FLOW_VERSION"));
        assertEquals(3600, step1.getTimeout().intValue());
        assertEquals(1, step1.getSecrets().size());
        assertTrue(step1.getSecrets().contains("my-secret"));

        assertTrue(step1.isAllowFailure());
        assertEquals("println(FLOW_WORKSPACE)\ntrue\n", step1.getCondition());

        // verify cache
        assertNotNull(step1.getCache());
        assertEquals(2, step1.getCache().getPaths().size());
        assertEquals("mycache", step1.getCache().getKey());
        assertEquals("./", step1.getCache().getPaths().get(0));
        assertEquals("vendor", step1.getCache().getPaths().get(1));

        RegularStepNode step2 = (RegularStepNode) steps.get(1);
        assertEquals("step2", step2.getName());
        assertEquals("echo 2", step2.getBash());
        assertEquals("echo powershell", step2.getPwsh());
        assertNull(step2.getTimeout());

        DockerOption dockerOption = step2.getDockers().get(0);
        assertNotNull(dockerOption);
        assertEquals("ubuntu:18.04", dockerOption.getImage());
        assertEquals("6400:6400", dockerOption.getPorts().get(0));
        assertEquals("2700:2700", dockerOption.getPorts().get(1));
        assertEquals("/bin/sh", dockerOption.getEntrypoint().get(0));
        assertEquals("host", dockerOption.getNetwork());
    }

    @Test
    public void should_get_correct_relationship_on_node_tree() {
        FlowNode root = YmlParser.load(content);
        NodeTree tree = NodeTree.create(root);
        assertEquals(root, tree.getRoot());

        // verify parent / child relationship
        Node step1 = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step-1")); // step-1 is default name
        assertNotNull(step1);
        assertEquals(root, step1.getParent());

        Node step2 = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step2"));
        assertNotNull(step2);
        assertTrue(step2.getChildren().isEmpty());
        assertEquals(root, step2.getParent());
        assertEquals(step2, step1.getNext().get(0));
    }

    @Test
    public void should_parse_yml_with_exports_filter() throws IOException {
        content = TestUtil.loadContent("v1/flow-with-exports.yml");
        FlowNode root = YmlParser.load(content);
        NodeTree tree = NodeTree.create(root);

        List<Node> first = tree.getRoot().getNext();
        RegularStepNode node = (RegularStepNode) first.get(0);

        assertEquals("step-1", node.getPath().name());
        assertEquals(2, node.getExports().size());
    }

    @Test
    public void should_parse_docker_and_dockers() throws IOException {
        content = TestUtil.loadContent("v1/step-with-dockers.yml");
        FlowNode root = YmlParser.load(content);
        NodeTree tree = NodeTree.create(root);

        RegularStepNode first = (RegularStepNode) root.getNext().get(0);
        assertEquals(1, first.getDockers().size());
        assertEquals("ubuntu:18.04", first.getDockers().get(0).getImage());
        assertTrue(first.getDockers().get(0).isRuntime());

        RegularStepNode second = (RegularStepNode) first.getNext().get(0);
        assertEquals(2, second.getDockers().size());

        assertEquals("ubuntu:18.04", second.getDockers().get(0).getImage());
        assertTrue(second.getDockers().get(0).isRuntime());

        assertEquals("mysql", second.getDockers().get(1).getImage());
        assertEquals("12345", second.getDockers().get(1).getEnvironment().get("MY_PW"));

        assertEquals(2, second.getDockers().get(1).getCommand().size());
        assertEquals("mysql", second.getDockers().get(1).getCommand().get(0));
        assertEquals("-hlocalhost", second.getDockers().get(1).getCommand().get(1));

        assertFalse(second.getDockers().get(1).isRuntime());
    }

    @Test
    public void should_throw_ex_when_runtime_has_command() throws IOException {
        content = TestUtil.loadContent("v1/runtime-with-command.yml");
        assertThrows(YmlException.class, () -> YmlParser.load(content));
    }

    @Test
    public void should_parse_step_in_step() throws IOException {
        content = TestUtil.loadContent("v1/step-in-step.yml");

        FlowNode root = YmlParser.load(content);
        assertEquals(3, root.getChildren().size());

        RegularStepNode step2 = (RegularStepNode) root.getChildren().get(1);
        assertEquals(root, step2.getParent());

        assertEquals(2, step2.getChildren().size());
        RegularStepNode step2_1 = (RegularStepNode) step2.getChildren().get(0);
        assertEquals(step2, step2_1.getParent());

        RegularStepNode step2_2 = (RegularStepNode) step2.getChildren().get(1);
        assertEquals(step2, step2_2.getParent());

        NodeTree tree = NodeTree.create(root);
        assertEquals(6, tree.numOfNode());

        NodePath step1Path = NodePath.create(DEFAULT_ROOT_NAME, "step-1");
        assertEquals("step2", tree.get(step1Path).getNext().get(0).getName());

        NodePath step2Path = NodePath.create(DEFAULT_ROOT_NAME, "step2");
        assertEquals("create test", tree.skip(step2Path).get(0).getName());

        NodePath step22Path = NodePath.create(DEFAULT_ROOT_NAME, "step2", "step-2-2");
        Node step3 = tree.skip(step22Path).get(0);
        assertNotNull(step3);
        assertEquals("create test", step3.getName());

        assertEquals(0, tree.skip(step3.getPath()).size());
    }

    @Test
    public void should_throw_ex_when_plugin_defined_in_parent_step() throws IOException {
        content = TestUtil.loadContent("v1/parent-step-with-plugin.yml");
        assertThrows(YmlException.class, () -> YmlParser.load(content));
    }

    @Test
    public void should_load_parallel_step_yaml() throws IOException {
        content = TestUtil.loadContent("v1/flow-parallel.yml");
        FlowNode root = YmlParser.load(content);
        assertNotNull(root);

        NodeTree tree = NodeTree.create(root);
        assertNotNull(tree);
        assertEquals(root, tree.getRoot());
        assertEquals(12, tree.numOfNode());
        assertEquals(2, tree.getSelectors().size());
        assertEquals(1, tree.getEnds().size());

        NodePath parallelPath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1");
        NodePath step2Path = NodePath.create(DEFAULT_ROOT_NAME, "step2");
        NodePath step3Path = NodePath.create(DEFAULT_ROOT_NAME, "step3");
        NodePath step3_1Path = NodePath.create(DEFAULT_ROOT_NAME, "step3", "step-3-1");
        NodePath step3_2Path = NodePath.create(DEFAULT_ROOT_NAME, "step3", "step-3-2");
        NodePath step4Path = NodePath.create(DEFAULT_ROOT_NAME, "step4");

        NodePath subflowAPath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-A");
        NodePath subflowA_APath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-A", "A");
        NodePath subflowA_BPath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-A", "B");

        NodePath subflowBPath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-B");
        NodePath subflowB_APath = NodePath.create(DEFAULT_ROOT_NAME, "parallel-1", "subflow-B", "A");

        // -------- validate next function --------
        List<Node> parallelStep = root.getNext();
        assertEquals(1, parallelStep.size());
        assertEquals(parallelPath, parallelStep.get(0).getPath());

        // parallel-1 next should be two subflows
        List<Node> subflows = parallelStep.get(0).getNext();
        assertEquals(2, subflows.size());

        Node subflowA = subflows.get(0);
        assertEquals(subflowAPath, subflowA.getPath());

        Node subflowB = subflows.get(1);
        assertEquals(subflowBPath, subflowB.getPath());

        // test subflow-A
        // next should be the first node subflow-A-A
        List<Node> subflowA_A = subflowA.getNext();
        assertEquals(1, subflowA_A.size());
        assertEquals(subflowA_APath, subflowA_A.get(0).getPath());

        // next should be the second node of subflow-A/B
        List<Node> subflowA_B = subflowA_A.get(0).getNext();
        assertEquals(1, subflowA_B.size());
        assertEquals(subflowA_BPath, subflowA_B.get(0).getPath());

        // subflow-A/B next should be step 2
        List<Node> step2 = subflowA_B.get(0).getNext();
        assertEquals(1, step2.size());
        assertEquals(step2Path, step2.get(0).getPath());

        // test subflow-B
        List<Node> subflowB_A = subflowB.getNext();
        assertEquals(1, subflowB_A.size());
        assertEquals(subflowB_APath, subflowB_A.get(0).getPath());

        // subflow-B/A next should be step2
        step2 = tree.get(subflowB_APath).getNext();
        assertEquals(1, step2.size());
        assertEquals(step2Path, step2.get(0).getPath());

        // step2 next should be step3
        List<Node> step3 = step2.get(0).getNext();
        assertEquals(1, step3.size());
        assertEquals(step3Path, step3.get(0).getPath());

        // step3 next should be step-3-1
        List<Node> step3_1 = step3.get(0).getNext();
        assertEquals(1, step3_1.size());
        assertEquals(step3_1Path, step3_1.get(0).getPath());

        // step-3-1 next should be step-3-2
        List<Node> step3_2 = step3_1.get(0).getNext();
        assertEquals(1, step3_2.size());
        assertEquals(step3_2Path, step3_2.get(0).getPath());

        // step-3-2 next should be step4
        List<Node> step4 = step3_2.get(0).getNext();
        assertEquals(1, step4.size());
        assertEquals(step4Path, step4.get(0).getPath());

        // -------- validate skip function --------
        assertEquals(step2Path, tree.skip(parallelPath).get(0).getPath());
        assertEquals(step3Path, tree.skip(step2Path).get(0).getPath());
        assertEquals(step4Path, tree.skip(step3Path).get(0).getPath());

        assertEquals(step3_2Path, tree.skip(step3_1Path).get(0).getPath());
        assertEquals(step4Path, tree.skip(step3_2Path).get(0).getPath());

        assertTrue(tree.skip(step4Path).isEmpty());

        assertEquals(step2Path, tree.skip(subflowAPath).get(0).getPath());
        assertEquals(step2Path, tree.skip(subflowBPath).get(0).getPath());

        assertEquals(subflowA_BPath, tree.skip(subflowA_APath).get(0).getPath());
        assertEquals(step2Path, tree.skip(subflowA_BPath).get(0).getPath());

        assertEquals(step2Path, tree.skip(subflowB_APath).get(0).getPath());
    }

    @Test
    public void should_load_and_get_post_steps() throws IOException {
        content = TestUtil.loadContent("v1/flow-with-post.yml");
        FlowNode root = YmlParser.load(content);
        assertNotNull(root);

        NodeTree tree = NodeTree.create(root);
        assertNotNull(tree);

        NodePath postOfSubA = NodePath.create("flow/parallel-2/subflow-A/subA-post-1");
        NodePath postOfSubC = NodePath.create("flow/parallel-3/subflow-C/Post-C");
        NodePath postOfSubD = NodePath.create("flow/parallel-3/subflow-D/Post-D");
        NodePath post1OfRoot = NodePath.create("flow/post-1");
        NodePath post2OfRoot = NodePath.create("flow/post-2");

        List<Node> nextFromRoot = tree.post(NodePath.create("flow"));
        assertEquals(1, nextFromRoot.size());
        assertEquals(postOfSubA, nextFromRoot.get(0).getPath());

        List<Node> nextFromSubflowC = tree.post(NodePath.create("flow/parallel-3/subflow-C/C"));
        assertEquals(2, nextFromSubflowC.size());

        Set<NodePath> postOfParallel3 = new HashSet<>(2);
        postOfParallel3.add(nextFromSubflowC.get(0).getPath());
        postOfParallel3.add(nextFromSubflowC.get(1).getPath());
        assertTrue(postOfParallel3.contains(postOfSubC));
        assertTrue(postOfParallel3.contains(postOfSubD));

        List<Node> nextFromSubA = tree.post(postOfSubA);
        assertEquals(2, nextFromSubA.size());

        postOfParallel3 = new HashSet<>(2);
        postOfParallel3.add(nextFromSubA.get(0).getPath());
        postOfParallel3.add(nextFromSubA.get(1).getPath());
        assertTrue(postOfParallel3.contains(postOfSubC));
        assertTrue(postOfParallel3.contains(postOfSubD));

        List<Node> nextFromSubC = tree.post(postOfSubC);
        assertEquals(1, nextFromSubC.size());
        assertEquals(post1OfRoot, nextFromSubC.get(0).getPath());

        List<Node> nextFromSubD = tree.post(postOfSubD);
        assertEquals(1, nextFromSubD.size());
        assertEquals(post1OfRoot, nextFromSubD.get(0).getPath());

        List<Node> nextPostFromRootPost1 = tree.post(post1OfRoot);
        assertEquals(1, nextPostFromRootPost1.size());
        assertEquals(post2OfRoot, nextPostFromRootPost1.get(0).getPath());

        // get prev post step
        Collection<Node> prevsOfPost1 = tree.prevs(nextFromSubD, false);
        assertEquals(1, prevsOfPost1.size());

        prevsOfPost1 = tree.prevs(nextFromSubD, true);
        assertEquals(2, prevsOfPost1.size());
    }
}
