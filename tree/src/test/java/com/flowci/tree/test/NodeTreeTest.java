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

import com.flowci.tree.*;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author yang
 */
public class NodeTreeTest {

    private NodeTree tree;

    @Before
    public void init() throws Exception {
        URL resource = getClass().getClassLoader().getResource("flow-with-after.yml");
        String content = Files.toString(new File(resource.getFile()), StandardCharsets.UTF_8);
        FlowNode root = YmlParser.load("default", content);
        tree = NodeTree.create(root);
    }

    @Test
    public void should_check_is_first_node() {
        Assert.assertTrue(tree.isFirst(NodePath.create("root/step-1")));
    }

    @Test
    public void should_get_after_node() {
        StepNode step = tree.next(tree.getRoot().getPath());
        Assert.assertNotNull(step);
        Assert.assertFalse(step.isAfter());
        Assert.assertEquals("step-1", step.getName());

        step = tree.next(NodePath.create("root/step-1"));
        Assert.assertNotNull(step);
        Assert.assertFalse(step.isAfter());
        Assert.assertEquals("step2", step.getName());

        // then: should get next final as null from last node
        step = tree.next(NodePath.create("root/step2"));
        Assert.assertNotNull(step);
        Assert.assertTrue(step.isAfter());
        Assert.assertEquals("step3", step.getName());
    }
}
