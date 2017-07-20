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
package com.flow.platform.api.service;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import javax.annotation.PostConstruct;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author yh@firim
 */

@FixMethodOrder(value = MethodSorters.JVM)
public class NodeServiceTest extends TestBase{

    @Autowired
    JobService jobService;

    @Autowired
    NodeService nodeService;

    @Autowired
    JobNodeService jobNodeService;

    NodeUtil nodeUtil;
    NodeUtil jobNodeUtil;

    @PostConstruct
    public void init(){
        nodeUtil = new NodeUtil(nodeService);
        jobNodeUtil = new NodeUtil(jobNodeService);
    }

    @Test
    public void should_node_initialized(){
        Flow flow1 = new Flow();
        flow1.setName("flow1");
        flow1.setScript("echo 1");
        flow1 = (Flow) nodeService.create(flow1);
        Assert.assertNotNull(flow1.getPath());
        Flow flow2 = (Flow) nodeService.find(flow1.getPath());
        Assert.assertNotNull(flow1.getPath(), flow2.getPath());
        flow2.setName("flow2");
        flow2 = (Flow) nodeService.update(flow2);
        Assert.assertEquals("flow2", flow2.getName());
    }

    @Test
    public void should_node_save(){
        Flow flow1 = new Flow();
        Step step1 = new Step();
        step1.setName("step1");
        Step step2 = new Step();
        step2.setName("step2");
        Step step3 = new Step();
        step3.setName("step3");

        step2.setNext(step3);
        step2.setPrev(step1);
        step1.setNext(step2);
        step3.setPrev(step2);

        step1 = (Step) nodeService.create(step1);
        step2 = (Step) nodeService.create(step2);
        step3 = (Step) nodeService.create(step3);
        flow1.getChildren().add(step1);
        flow1.getChildren().add(step2);
        flow1.getChildren().add(step3);
        flow1 = (Flow) nodeService.create(flow1);

        Assert.assertNotNull(flow1.getPath());
        Assert.assertEquals(3, flow1.getChildren().size());
        Step stepFirst = (Step) flow1.getChildren().get(0);
        Assert.assertEquals("step1", stepFirst.getName());
        Assert.assertEquals("step2", stepFirst.getNext().getName());
        Assert.assertEquals(null, stepFirst.getPrev());
    }


    @Test
    public void should_delete(){
        Flow flow1 = new Flow();
        flow1.setName("flow1");
        flow1 = (Flow) nodeService.create(flow1);
        nodeService.delete(flow1);
        Assert.assertEquals(null, nodeService.find(flow1.getPath()));
    }

    @Test
    public void should_equal_step(){
        Step step1 = new Step();
        step1.setPlugin("xxxxxx");
        step1 = (Step) nodeService.create(step1);
        Assert.assertEquals("xxxxxx", ((Step)nodeService.find(step1.getPath())).getPlugin());
    }


    @Test
    public void should_instance_equal(){
        Step step1 = new Step();
        Node node = step1;
        Step step2 = (Step) node;
        Assert.assertEquals(true, node instanceof Step);

        JobStep jobstep = new JobStep();
        jobstep.setCmdId("00000000000000000");
        jobstep = (JobStep) nodeService.create(jobstep);
        Assert.assertEquals("00000000000000000", ((JobStep)nodeService.find(jobstep.getPath())).getCmdId());
        Node node1 = nodeService.find(jobstep.getPath());
        Assert.assertEquals(true, node1 instanceof JobStep);
    }

    @Test
    public void should_list_children(){
        Step step1 = new Step();
        step1.setName("step1");
        Step step2 = new Step();
        step2.setName("step2");
        step1 = (Step) nodeService.create(step1);
        step2 = (Step) nodeService.create(step2);
        Flow flow = new Flow();
        flow = (Flow) nodeService.create(flow);
        step1.setNextId(step2.getPath());
        step2.setPrevId(step1.getPath());
        step1.setParentId(flow.getPath());
        step2.setParentId(flow.getPath());
        Assert.assertEquals("step1", nodeService.prevNode(step2).getName());
        Assert.assertEquals("step2", nodeService.nextNode(step1).getName());
        Assert.assertEquals(2, nodeService.listChildrenByNode(flow).size());
        Assert.assertEquals(2, nodeUtil.allChildren(flow).size());
    }
}
