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
package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class NodeServiceTest extends TestBase{
    @Autowired
    NodeService nodeService;

    @Test
    public void should_save_node(){
        Flow flow = new Flow();
        flow.setPath("/flow");
        flow.setName("flow");
        nodeService.save(flow);
        Assert.assertEquals(flow.getPath(), nodeService.find(flow.getPath()).getPath());
    }

    @Test
    public void should_create_and_get_node(){
        Flow flow = new Flow();
        flow.setName("flow");
        flow.setPath("/flow");
        Step step1 = new Step();
        step1.setParent(flow);
        step1.setName("step1");
        step1.setPath("/flow/step1");
        flow.getChildren().add(step1);
        Node node = nodeService.create(flow);
        NodeUtil.recurse(flow, item->{
            Assert.assertEquals(item.getName(), nodeService.find(item.getPath()).getName());
        });
    }
}
