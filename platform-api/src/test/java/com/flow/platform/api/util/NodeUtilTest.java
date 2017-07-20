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
 * @author gyfirim
 */
public class NodeUtilTest extends TestBase {

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeService jobNodeService;

    NodeUtil nodeUtil;
    NodeUtil jobNodeUtil;

    @PostConstruct
    public void init(){
        nodeUtil = new NodeUtil(nodeService);
        jobNodeUtil = new NodeUtil(jobNodeService);
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
        nodeService.create(step1);
        nodeService.create(step2);
        nodeService.create(step3);
        nodeService.create(step4);
        nodeService.create(step5);
        nodeService.create(step6);
        nodeService.create(step7);

        step4.setParentId(step1.getPath());
        step5.setParentId(step1.getPath());
        step4.setNextId(step5.getPath());
        step5.setPrevId(step4.getPath());

        step6.setParentId(step2.getPath());
        step7.setParentId(step6.getPath());

        step1.setNextId(step2.getPath());
        step2.setPrevId(step1.getPath());
        step2.setNextId(step3.getPath());
        step3.setPrevId(step2.getPath());

        Flow flow = new Flow();
        flow.setName("flow1");
        nodeService.create(flow);
        step1.setParentId(flow.getPath());
        step2.setParentId(flow.getPath());
        step3.setParentId(flow.getPath());

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
