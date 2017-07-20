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
import com.flow.platform.api.test.TestBase;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gyfirim
 */
public class NodeUtilTest extends TestBase {

    @Autowired
    private NodeUtil nodeUtil;
    @Test
    public void should_get_allChildren_first(){
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
        step1.setNext(step2);
        step2.setNext(step3);
        step3.setNext(step4);
        step4.setNext(step5);
        step5.setNext(step6);
        step6.setNext(step7);
        Flow flow = new Flow();
        flow.setName("flow1");
        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        flow.getChildren().add(step3);
        flow.getChildren().add(step4);
        flow.getChildren().add(step5);
        flow.getChildren().add(step6);
        flow.getChildren().add(step7);
        try{
            List<Node> nodeList = nodeUtil.allChildren(flow);
            Assert.assertEquals(7, nodeList.size());
        }catch (RuntimeException e){
        }
    }


    @Test
    public void should_get_allChildren_second(){
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

        step1.getChildren().add(step4);
        step1.getChildren().add(step5);
        step4.setParent(step1);
        step5.setParent(step1);
        step4.setNext(step5);
        step5.setPrev(step4);

        step2.getChildren().add(step6);
        step6.setParent(step2);
        step6.getChildren().add(step7);
        step7.setParent(step6);

        step1.setNext(step2);
        step2.setPrev(step1);
        step2.setNext(step3);
        step3.setPrev(step2);

        Flow flow = new Flow();
        flow.setName("flow1");
        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        flow.getChildren().add(step3);
        step1.setParent(flow);
        step2.setParent(flow);
        step3.setParent(flow);

        List<Node> nodeList = nodeUtil.allChildren(flow);
        Assert.assertEquals(7, nodeList.size());

        Assert.assertEquals(step1.getName(), nodeUtil.prevNodeFromAllChildren(step7).getName());
        Assert.assertEquals(step2.getName(), nodeUtil.prevNodeFromAllChildren(step3).getName());
        Assert.assertEquals(step7.getName(), nodeUtil.prevNodeFromAllChildren(step6).getName());
        Assert.assertEquals(step4.getName(), nodeUtil.prevNodeFromAllChildren(step5).getName());
        Assert.assertEquals(null, nodeUtil.prevNodeFromAllChildren(step4));

        Assert.assertEquals(step1.getName(), nodeUtil.nextNodeFromAllChildren(step5).getName());
        Assert.assertEquals(step6.getName(), nodeUtil.nextNodeFromAllChildren(step7).getName());
        Assert.assertEquals(step2.getName(), nodeUtil.nextNodeFromAllChildren(step6).getName());
        Assert.assertEquals(step3.getName(), nodeUtil.nextNodeFromAllChildren(step2).getName());
        Assert.assertEquals(null, nodeUtil.nextNodeFromAllChildren(step3));

    }



}
