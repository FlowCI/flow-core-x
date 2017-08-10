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
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class NodeServiceTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Test
    public void should_create_node_by_obj_tree() {
        // given: create node
        Flow flow = new Flow("/flow", "flow");
        flow.getChildren().add(new Step("/flow/first", "first"));
        flow.getChildren().add(new Step("/flow/second", "second"));

        NodeUtil.buildNodeRelation(flow);
        Assert.assertEquals(flow, flow.getChildren().get(0).getParent());
        Assert.assertEquals(flow, flow.getChildren().get(1).getParent());

        Assert.assertEquals(flow.getChildren().get(1), flow.getChildren().get(0).getNext());
        Assert.assertEquals(null, flow.getChildren().get(0).getPrev());

        Assert.assertEquals(flow.getChildren().get(0), flow.getChildren().get(1).getPrev());
        Assert.assertEquals(null, flow.getChildren().get(1).getNext());

        // when: create node
        Node root = nodeService.create(flow);
        Assert.assertEquals(flow, root);

        Assert.assertEquals(flow.getPath(), nodeService.find(flow.getPath()).getPath());
    }

    @Test
    public void should_create_node_by_yml() throws Throwable {
        // when:
        String resourceContent = getResourceContent("demo_flow.yaml");
        Node root = nodeService.create(resourceContent);

        // then:
        Flow saved = flowDao.get(root.getPath());
        Assert.assertNotNull(saved);
        Assert.assertTrue(nodeService.isExistedFlow(root.getName()));
        Assert.assertEquals(root, saved);
        Assert.assertEquals("flow1", saved.getName());
        Assert.assertEquals("/flow1", saved.getPath());

        root = nodeService.find(saved.getPath());
        Step step1 = (Step) root.getChildren().get(0);
        Assert.assertEquals("/flow1/step1", step1.getPath());
        Step step2 = (Step) root.getChildren().get(1);
        Assert.assertEquals("/flow1/step2", step2.getPath());

        YmlStorage yaml = ymlStorageDao.get(root.getPath());
        Assert.assertNotNull(yaml);
        Assert.assertEquals(resourceContent, yaml.getFile());
    }

    @Test
    public void should_create_and_get_node() {
        Flow flow = new Flow("/flow", "flow");
        Step step1 = new Step("/flow/step1", "step1");
        flow.getChildren().add(step1);
        step1.setParent(flow);
        nodeService.create(flow);
        NodeUtil.recurse(flow, item -> {
            Assert.assertEquals(item.getName(), nodeService.find(item.getPath()).getName());
        });
    }

    @Test
    public void should_create_empty_flow() {
        // when:
        String flowName = "default";
        Flow emptyFlow = nodeService.createEmptyFlow(flowName);

        // then:
        Flow loaded = flowDao.get(emptyFlow.getPath());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(emptyFlow, loaded);
        Assert.assertEquals(0, loaded.getChildren().size());

        // should with empty yml
        Assert.assertNull(ymlStorageDao.get(loaded.getPath()));
    }
}
