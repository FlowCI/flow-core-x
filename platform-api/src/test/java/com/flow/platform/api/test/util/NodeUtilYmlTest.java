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
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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

    @Test
    public void should_parse_node_yml() throws Throwable {
        final String flow = "yml-test";
        Node root = NodeUtil.buildFromYml(ymlContent, flow);
        Assert.assertEquals(flow, root.getName());
        Assert.assertEquals(flow, root.getPath());

        String parsedYml = NodeUtil.parseToYml(root);
        Assert.assertEquals(ymlContent, parsedYml);
    }
}