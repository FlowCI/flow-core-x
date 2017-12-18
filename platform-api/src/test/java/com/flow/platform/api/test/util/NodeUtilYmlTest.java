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

package com.flow.platform.api.test.util;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * @author yang
 */
public class NodeUtilYmlTest {

    private String ymlContent;

    @Before
    public void before() throws Throwable {
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource("yml/flow.yaml");
        ymlContent = Files.toString(new File(resource.getFile()), AppConfig.DEFAULT_CHARSET);
    }

    @Test(expected = YmlException.class)
    public void should_raise_yml_exception_if_incorrect_format() {
        NodeUtil.buildFromYml("hello test", "flow");
    }

    @Test
    public void should_create_node_by_file() throws IOException {
        Node node = NodeUtil.buildFromYml(ymlContent, "flow1");

        // verify flow
        Assert.assertEquals("flow1", node.getName());
        Assert.assertEquals("flow1", node.getPath());

        // verify flow envs
        Assert.assertEquals(2, node.getEnvs().size());
        Assert.assertEquals("echo hello", node.getEnvs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", node.getEnvs().get("FLOW_VERSION"));

        // verify steps
        Node root =  node;
        List<Node> steps = root.getChildren();
        Assert.assertEquals(2, steps.size());

        Assert.assertEquals("step1", steps.get(0).getName());
        Assert.assertEquals("flow1/step1", steps.get(0).getPath());

        Node step1 = steps.get(0);
        Assert.assertEquals("echo step", step1.getEnvs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo step version", step1.getEnvs().get("FLOW_VERSION"));
        Assert.assertNotNull(step1.getConditionScript());

        Node step2 = steps.get(1);
        Assert.assertEquals("step2", step2.getName());
        Assert.assertEquals("flow1/step2", step2.getPath());
        Assert.assertNull(step2.getConditionScript());

        // verify parent node relationship
        Assert.assertEquals(root, step1.getParent());
        Assert.assertEquals(root, step2.getParent());

        // verify prev next node relationship
        Assert.assertEquals(step2, step1.getNext());
        Assert.assertEquals(step1, step2.getPrev());
    }

    @Test
    public void should_create_node_by_string() throws Throwable {
        Node node = NodeUtil.buildFromYml(ymlContent, "flow");
        Assert.assertEquals("flow", node.getName());

        String yml = new Yaml().dump(node);
        Assert.assertNotNull(yml);
    }

    @Test(expected = YmlException.class)
    public void should_raise_error_when_parse_node_with_same_name() throws Throwable {
        final String flow = "yml-test";

        // when: build children with same name
        Node root = new Node(flow, flow);
        root.getChildren().add(new Node(null, "step1"));
        root.getChildren().add(new Node(null, "step1"));

        // then: should throw YmlException: The step name 'step1'is not unique
        NodeUtil.parseToYml(root);
    }

    @Test
    public void should_parse_node_to_yml() throws Throwable {
        // given:
        final String flow = "yml-test";
        Node root = NodeUtil.buildFromYml(ymlContent, flow);
        Assert.assertEquals(flow, root.getName());
        Assert.assertEquals(flow, root.getPath());

        // when: parse root to yml
        String parsedYml = NodeUtil.parseToYml(root);

        // then:
        Node parsedRoot = NodeUtil.buildFromYml(parsedYml, flow);
        Assert.assertNotNull(parsedRoot);
        Assert.assertEquals(flow, parsedRoot.getName());
        Assert.assertEquals(flow, parsedRoot.getPath());
        Assert.assertEquals(2, parsedRoot.getChildren().size());

        Node step1 = parsedRoot.getChildren().get(0);
        Assert.assertEquals("println(FLOW_WORKSPACE)\ntrue\n", step1.getConditionScript());
        Assert.assertEquals(true, step1.getAllowFailure());
        Assert.assertEquals("step1", step1.getName());
        Assert.assertEquals(PathUtil.build(flow, "step1"), step1.getPath());
        Assert.assertEquals(2, step1.getChildren().size());

        Node step1InsideStep1 = step1.getChildren().get(0);
        Assert.assertEquals("step11", step1InsideStep1.getName());
        Assert.assertEquals(PathUtil.build(flow, "step1", "step11"), step1InsideStep1.getPath());
        Assert.assertEquals(false, step1InsideStep1.getAllowFailure());
        Assert.assertEquals("echo 1", step1InsideStep1.getScript());

        Node step2InsideStep2 = step1.getChildren().get(1);
        Assert.assertEquals("step12", step2InsideStep2.getName());
        Assert.assertEquals(PathUtil.build(flow, "step1", "step12"), step2InsideStep2.getPath());
        Assert.assertEquals(false, step2InsideStep2.getAllowFailure());
        Assert.assertEquals("echo 2", step2InsideStep2.getScript());

        Node step2 = parsedRoot.getChildren().get(1);
        Assert.assertEquals("step2", step2.getName());
        Assert.assertEquals(PathUtil.build(flow, "step2"), step2.getPath());
        Assert.assertEquals(false, step2.getAllowFailure());
        Assert.assertEquals("echo 2", step2.getScript());
    }

    @Test(expected = YmlException.class)
    public void should_raise_error_if_step_name_missing_parse_node_to_yml() throws Throwable {
        String flow = "yml-flow";
        Node root = new Node(flow, flow);
        Node step = new Node(null, null);
        step.setScript("xxx");
        root.getChildren().add(step);

        String yml = NodeUtil.parseToYml(root);
        Assert.assertNotNull(yml);
    }

    @Test(expected = YmlException.class)
    public void should_raise_error_if_step_script_and_plugin_defined_in_both() throws Throwable {
        String flow = "yml-flow";
        Node root = new Node(flow, flow);
        Node step = new Node(null, "step1");
        step.setScript("echo 1");
        step.setPlugin("fir-plugin");
        root.getChildren().add(step);

        String yml = NodeUtil.parseToYml(root);
        Assert.assertNotNull(yml);
    }
}