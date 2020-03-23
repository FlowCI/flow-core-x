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

import com.flowci.exception.YmlException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        YmlParser.load("root", content);
    }

    @Test
    public void should_get_node_from_yml() {
        Node root = YmlParser.load("root", content);

        // verify flow
        Assert.assertEquals("* * * * *", root.getCron());
        Assert.assertEquals("root", root.getName());
        Assert.assertEquals("echo hello", root.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", root.getEnv("FLOW_VERSION"));

        Assert.assertTrue(root.getSelector().getTags().contains("ios"));
        Assert.assertTrue(root.getSelector().getTags().contains("local"));

        Assert.assertEquals(3, root.getTrigger().getBranches().size());
        Assert.assertEquals(1, root.getTrigger().getTags().size());

        // verify steps
        List<Node> steps = root.getChildren();
        Assert.assertEquals(2, steps.size());

        Node step1 = steps.get(0);
        Assert.assertEquals("step-1", step1.getName()); // step-1 is default name
        Assert.assertEquals("echo step", step1.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo step version", step1.getEnv("FLOW_VERSION"));

        Assert.assertTrue(step1.isAllowFailure());
        Assert.assertFalse(step1.isTail());
        Assert.assertEquals("println(FLOW_WORKSPACE)\ntrue\n", step1.getBefore());

        Node step2 = steps.get(1);
        Assert.assertEquals("step2", step2.getName());
    }

    @Test
    public void should_get_correct_relationship_on_node_tree() {
        Node root = YmlParser.load("hello", content);
        NodeTree tree = NodeTree.create(root);
        Assert.assertEquals(root, tree.getRoot());

        // verify parent / child relationship
        Node step1 = tree.get(NodePath.create("root/step-1")); // step-1 is default name
        Assert.assertNotNull(step1);
        Assert.assertEquals(root, step1.getParent());

        Node step2 = tree.get(NodePath.create("root/step2"));
        Assert.assertNotNull(step2);
        Assert.assertTrue(step2.getChildren().isEmpty());
        Assert.assertEquals(root, step2.getParent());

        // verify next / previous relationship
        Assert.assertEquals(step2, tree.next(step1.getPath()));
        Assert.assertNull(tree.next(step2.getPath()));
    }

    @Test
    public void should_parse_to_yml_from_node() {
        Node root = YmlParser.load("default", content);
        String parsed = YmlParser.parse(root);
        Assert.assertNotNull(parsed);
    }

    @Test
    public void should_parse_yml_with_exports_filter() throws IOException {
        content = loadContent("flow-with-exports.yml");
        Node root = YmlParser.load("default", content);
        NodeTree tree = NodeTree.create(root);

        Node first = tree.next(tree.getRoot().getPath());
        Assert.assertEquals("step-1", first.getPath().name());

        Assert.assertEquals(2, first.getExports().size());
    }

    private String loadContent(String resource) throws IOException {
        ClassLoader classLoader = YmlParserTest.class.getClassLoader();
        URL url = classLoader.getResource(resource);
        return Files.toString(new File(url.getFile()), Charset.forName("UTF-8"));
    }
}
