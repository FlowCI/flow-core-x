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
package com.flow.platform.api.util;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import java.util.List;
import javax.annotation.PostConstruct;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class NdUtilTest extends TestBase {



    @Test
    public void should_depNodes_complex(){
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

        List<Node> nodes = NdUtil.deptNodes(flow);
        String out = "";
        for (Node node : nodes) {
            System.out.println(node.getName());
            out = out + node.getName() + ";";
        }
        System.out.println(out);
        Assert.assertEquals("step3;step7;step8;step4;step1;step5;step6;step2;flow;", out);
    }

    @Test
    public void should_depNodes_simple(){
        Step step1 = new Step();
        step1.setName("step1");
        Step step2 = new Step();
        step2.setName("step2");
        Flow flow = new Flow();
        flow.setName("flow");
        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setPrev(step1);

        List<Node> nodes = NdUtil.deptNodes(flow);
        String out = "";
        for (Node node : nodes) {
            System.out.println(node.getName());
            out = out + node.getName() + ";";
        }
        System.out.println(out);
        Assert.assertEquals("step1;step2;flow;", out);
    }

    @Test
    public void should_depNodes_one_node(){
        Step step1 = new Step();
        step1.setName("step1");
        Flow flow = new Flow();
        flow.setName("flow");
        flow.getChildren().add(step1);
        step1.setParent(flow);

        List<Node> nodes = NdUtil.deptNodes(step1);
        String out = "";
        for (Node node : nodes) {
            System.out.println(node.getName());
            out = out + node.getName() + ";";
        }
        System.out.println(out);
        Assert.assertEquals("step1;", out);
    }
}
