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
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.NodeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobNodeServiceTest extends TestBase {

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private NodeService nodeService;

    @Test
    public void should_save_node() {
        JobFlow flow = new JobFlow("/flow", "flow");
        flow.setExitCode(1);

        jobNodeService.save(flow);
        Assert.assertEquals(flow.getExitCode(), ((JobNode) jobNodeService.find(flow.getPath())).getExitCode());
    }

    @Test
    public void should_create_and_get_node() {
        JobFlow flow = new JobFlow("/flow", "flow");
        JobStep step1 = new JobStep("/flow/step1", "step1");

        flow.setDuration((long) 10);
        step1.setParent(flow);
        step1.setDuration((long) 10);
        flow.getChildren().add(step1);

        jobNodeService.create(flow);
        NodeUtil.recurse(flow, item -> {
            Assert.assertEquals(((JobNode) item).getDuration(),
                jobNodeService.find(flow.getPath()).getDuration());
        });
    }


    @Test
    public void should_copy_node() {
        Flow flow = new Flow("/flow", "flow");

        Step step1 = new Step("/flow/step1", "step1");
        Step step2 = new Step("/flow/step2", "step2");

        step1.setPlugin("step1");
        step1.setAllowFailure(true);
        step2.setPlugin("step2");
        step2.setAllowFailure(true);

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setParent(step1);

        nodeService.create(flow);

        JobFlow jobFlow = (JobFlow) jobNodeService.createJobNode(flow.getPath());
        for (Object item : jobFlow.getChildren()) {
            Assert.assertEquals(((Node) item).getPath(), jobNodeService.find(((Node) item).getPath()).getPath());
        }
    }

    @Test
    public void should_copy_node_simple() {
        // zero node

        Flow flow = new Flow("/flow", "flow");

        nodeService.create(flow);

        JobFlow jobFlow = (JobFlow) jobNodeService.createJobNode(flow.getPath());

        jobFlow = (JobFlow) jobNodeService.find(jobFlow.getPath());
        for (Object item : jobFlow.getChildren()) {
            Assert.assertEquals(((Node) item).getPath(), jobNodeService.find(((Node) item).getPath()).getPath());
        }
    }

}
