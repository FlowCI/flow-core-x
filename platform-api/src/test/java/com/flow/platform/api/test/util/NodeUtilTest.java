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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
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
        Step step1 = new Step();
        step1.setName("step1");
        Step step2 = new Step();
        step2.setName("step2");
        Step step3 = new Step();
        step3.setName("step3");
        Step step4 = new Step();
        step4.setName("step4");
        Step step5 = new Step();
        step5.setName("step5");
        Step step6 = new Step();
        step6.setName("step6");
        Step step7 = new Step();
        step7.setName("step7");
        Step step8 = new Step();
        step8.setName("step8");
        Flow flow = new Flow();
        flow.setName("flow");
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
    public void should_flat() {
        List<Node> nodes = NodeUtil.flat(initNodes());
        StringBuffer out = new StringBuffer("");

        for (Node node : nodes) {
            out.append(node.getName()).append(";");
        }
        Assert.assertEquals("step3;step7;step8;step4;step1;step5;step6;step2;flow;", out.toString());

    }

    @Test
    public void should_recurse() {
        Node node = initNodes();
        List<Node> nodeList = new LinkedList<>();
        NodeUtil.recurse(node, item -> {
            nodeList.add(item);
        });
        StringBuffer out = new StringBuffer("");
        for (Node iNode : nodeList) {
            out.append(iNode.getName()).append(";");
        }
        Assert.assertEquals("step3;step7;step8;step4;step1;step5;step6;step2;flow;", out.toString());
    }

    @Test
    public void should_find_root_simple() {
        Flow flow = new Flow();
        flow.setName("flow");
        Step step1 = new Step();
        step1.setName("step1");
        Step step2 = new Step();
        step2.setName("step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setNext(step2);
        step2.setPrev(step1);

        step1.setParent(flow);
        step2.setParent(flow);

        Assert.assertEquals("flow", NodeUtil.findRootNode(step1).getName());
        Assert.assertEquals("flow", NodeUtil.findRootNode(step2).getName());
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
        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/step1");
        Step step2 = new Step();
        step2.setName("step2");
        step2.setPath("/step2");
        Step step3 = new Step();
        step3.setName("step3");
        step3.setPath("/step3");
        Step step4 = new Step();
        step4.setName("step4");
        step4.setPath("/step4");
        Step step5 = new Step();
        step5.setName("step5");
        step5.setPath("/step5");
        Step step6 = new Step();
        step6.setName("step6");
        step6.setPath("/step6");
        Step step7 = new Step();
        step7.setName("step7");
        step7.setPath("/step7");
        Step step8 = new Step();
        step8.setName("step8");
        step8.setPath("/step8");
        Flow flow = new Flow();
        flow.setName("flow");
        flow.setPath("/flow");
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

        Assert.assertEquals(null, NodeUtil.prev(step3));
        Assert.assertEquals(step3, NodeUtil.prev(step7));
        Assert.assertEquals(step4, NodeUtil.prev(step1));
        Assert.assertEquals(step1, NodeUtil.prev(step5));
        Assert.assertEquals(step6, NodeUtil.prev(step2));
    }

    @Test
    public void should_equal_next_node() {
        Flow flow = new Flow();
        flow.setName("flow");
        flow.setPath("/flow");
        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/step1");
        Step step2 = new Step();
        step2.setName("step2");
        step2.setPath("/step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setNext(step2);
        step2.setPrev(step1);

        step1.setParent(flow);
        step2.setParent(flow);

        Assert.assertEquals(null, NodeUtil.next(step2));
        Assert.assertEquals(step1, NodeUtil.next(flow));
        Assert.assertEquals(step2, NodeUtil.next(step1));
    }

    @Test
    public void should_equal_next_one_node() {
        Flow flow = new Flow();
        flow.setName("flow");
        flow.setPath("/flow");

        Assert.assertEquals(null, NodeUtil.next(flow));
        Assert.assertEquals(null, NodeUtil.prev(flow));

        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/step1");

        flow.getChildren().add(step1);
        step1.setParent(flow);

        Assert.assertEquals(step1, NodeUtil.next(flow));
        Assert.assertEquals(null, NodeUtil.prev(flow));
        Assert.assertEquals(null, NodeUtil.next(step1));
        Assert.assertEquals(null, NodeUtil.prev(step1));
    }
}
