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
public class NodeTreeTest {

    private NodeTree tree;

    @Before
    public void init() throws Exception {
        URL resource = getClass().getClassLoader().getResource("flow-with-final.yml");
        String content = Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
        Node root = YmlParser.load("default", content);
        tree = NodeTree.create(root);
    }

    @Test
    public void should_check_first_or_last_node() {
        Assert.assertTrue(tree.isFirst(NodePath.create("root/step-1")));
        Assert.assertTrue(tree.isLast(NodePath.create("root/step3")));
    }

    @Test
    public void should_move_all_final_nodes_to_the_tail() {
        List<Node> ordered = tree.getOrdered();
        Assert.assertEquals("step3", ordered.get(ordered.size() - 1).getName());
    }

    @Test
    public void should_get_next_final_node() {
        // then: should get next final from root
        Node nextFinalNode = tree.nextFinal(tree.getRoot().getPath());
        Assert.assertNotNull(nextFinalNode);
        Assert.assertEquals("step3", nextFinalNode.getName());

        // then: should get next final from step
        nextFinalNode = tree.nextFinal(NodePath.create("root/step-1"));
        Assert.assertNotNull(nextFinalNode);
        Assert.assertEquals("step3", nextFinalNode.getName());

        // then: should get next final as null from last node
        nextFinalNode = tree.nextFinal(NodePath.create("root/step3"));
        Assert.assertNull(nextFinalNode);
    }
}
