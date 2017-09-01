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

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class NodeUtilTest extends TestBase {

    public Node initNodes() {
        Flow flow = new Flow("flow", "flow");

        Step step1 = new Step("step1", "step1");
        Step step2 = new Step("step2", "step2");
        Step step3 = new Step("step3", "step3");
        Step step4 = new Step("step4", "step4");
        Step step5 = new Step("step5", "step5");
        Step step6 = new Step("step6", "step6");
        Step step7 = new Step("step7", "step7");
        Step step8 = new Step("step8", "step8");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setPrev(step1);

        step1.getChildren().add(step3);
        step1.getChildren().add(step4);
        step3.setParent(step1);
        step4.setParent(step1);
        step3.setNext(step4);
        step4.setPrev(step3);

        step4.getChildren().add(step7);
        step4.getChildren().add(step8);
        step7.setParent(step4);
        step8.setParent(step4);
        step7.setNext(step8);
        step8.setPrev(step7);

        step2.getChildren().add(step5);
        step2.getChildren().add(step6);
        step5.setParent(step2);
        step6.setParent(step2);
        step5.setNext(step6);
        step6.setPrev(step5);
        return flow;
    }

    @Test
    public void should_build_node_relation() {
        // when: init node tree
        Node<Step> root = new Node<>();
        root.setName("root");

        Step childFirst = new Step();
        childFirst.setName("child-1");

        Step childSecond = new Step();
        childSecond.setName("child-2");

        Step subOfChildSecond = new Step();
        subOfChildSecond.setName("child-2-1");

        root.getChildren().add(childFirst);
        root.getChildren().add(childSecond);

        childSecond.getChildren().add(subOfChildSecond);

        NodeUtil.buildNodeRelation(root);

        // then: verify path of nodes
        Assert.assertEquals("root", root.getPath());
        Assert.assertEquals("root/child-1", childFirst.getPath());
        Assert.assertEquals("root/child-2", childSecond.getPath());
        Assert.assertEquals("root/child-2/child-2-1", subOfChildSecond.getPath());

        // then: verify relation
        Assert.assertEquals(childSecond, subOfChildSecond.getParent());
        Assert.assertEquals(null, subOfChildSecond.getPrev());
        Assert.assertEquals(null, subOfChildSecond.getNext());

        Assert.assertEquals(root, childFirst.getParent());
        Assert.assertEquals(root, childSecond.getParent());

        Assert.assertEquals(childSecond, childFirst.getNext());
        Assert.assertEquals(childFirst, childSecond.getPrev());
    }

    @Test
    public void should_flat() {
        List<Node> nodes = NodeUtil.flat(initNodes());
        StringBuilder out = new StringBuilder("");

        for (Node node : nodes) {
            out.append(node.getName()).append(";");
        }

        Assert.assertEquals("step3;step7;step8;step4;step1;step5;step6;step2;flow;", out.toString());
    }

    @Test
    public void should_recurse() {
        Node node = initNodes();
        List<Node> nodeList = new LinkedList<>();
        NodeUtil.recurse(node, nodeList::add);

        StringBuilder out = new StringBuilder("");
        for (Node iNode : nodeList) {
            out.append(iNode.getName()).append(";");
        }

        Assert.assertEquals("step3;step7;step8;step4;step1;step5;step6;step2;flow;", out.toString());
    }

    @Test
    public void should_find_root_simple() {
        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setNext(step2);
        step2.setPrev(step1);

        step1.setParent(flow);
        step2.setParent(flow);

        Assert.assertEquals("/flow", NodeUtil.findRootNode(step1).getName());
        Assert.assertEquals("/flow", NodeUtil.findRootNode(step2).getName());
    }

    @Test
    public void should_find_root_complex() {
        Node node = initNodes();

        Assert.assertEquals("flow", NodeUtil.findRootNode(node).getName());
        NodeUtil.recurse(node, item -> {
            Assert.assertEquals("flow", NodeUtil.findRootNode(item).getName());
        });
    }

    @Test
    public void should_equal_prev_node() {
        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");
        Step step3 = new Step("step3", "/flow/step3");
        Step step4 = new Step("step4", "/flow/step4");
        Step step5 = new Step("step5", "/flow/step5");
        Step step6 = new Step("step6", "/flow/step6");
        Step step7 = new Step("step7", "/flow/step7");
        Step step8 = new Step("step8", "/flow/step8");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setPrev(step1);

        step1.getChildren().add(step3);
        step1.getChildren().add(step4);
        step3.setParent(step1);
        step4.setParent(step1);
        step3.setNext(step4);
        step4.setPrev(step3);

        step4.getChildren().add(step7);
        step4.getChildren().add(step8);
        step7.setParent(step4);
        step8.setParent(step4);
        step7.setNext(step8);
        step8.setPrev(step7);

        step2.getChildren().add(step5);
        step2.getChildren().add(step6);
        step5.setParent(step2);
        step6.setParent(step2);
        step5.setNext(step6);
        step6.setPrev(step5);

        List<Node> ordered = NodeUtil.flat(flow);

        Assert.assertEquals(null, NodeUtil.prev(step3, ordered));
        Assert.assertEquals(step3, NodeUtil.prev(step7, ordered));
        Assert.assertEquals(step4, NodeUtil.prev(step1, ordered));
        Assert.assertEquals(step1, NodeUtil.prev(step5, ordered));
        Assert.assertEquals(step6, NodeUtil.prev(step2, ordered));
    }

    @Test
    public void should_equal_next_node() {
        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setNext(step2);
        step2.setPrev(step1);

        step1.setParent(flow);
        step2.setParent(flow);

        List<Node> ordered = NodeUtil.flat(flow);
        ordered.remove(flow);

        Assert.assertEquals(null, NodeUtil.next(step2, ordered));
        Assert.assertEquals(step1, ordered.get(0));
        Assert.assertEquals(step2, NodeUtil.next(step1, ordered));
    }

    @Test
    public void should_equal_next_one_node() {
        Flow flow = new Flow("flow", "/flow");
        List<Node> ordered = NodeUtil.flat(flow);

        Assert.assertEquals(null, NodeUtil.next(flow, ordered));
        Assert.assertEquals(null, NodeUtil.prev(flow, ordered));

        Step step1 = new Step("step1", "/flow/step1");

        flow.getChildren().add(step1);
        step1.setParent(flow);

        ordered = NodeUtil.flat(flow);
        ordered.remove(flow);

        Assert.assertEquals(step1, ordered.get(0));
        Assert.assertEquals(null, NodeUtil.prev(flow, ordered));
        Assert.assertEquals(null, NodeUtil.next(step1, ordered));
        Assert.assertEquals(null, NodeUtil.prev(step1, ordered));
    }
}
