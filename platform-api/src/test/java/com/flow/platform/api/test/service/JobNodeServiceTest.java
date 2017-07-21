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
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeService;
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
    JobNodeService jobNodeService;
    @Test
    public void should_save_node(){
        JobFlow flow = new JobFlow();
        flow.setPath("/flow");
        flow.setName("flow");
        flow.setExitCode(1);
        jobNodeService.save(flow);
        Assert.assertEquals(flow.getExitCode(), ((JobNode)jobNodeService.find(flow.getPath())).getExitCode());
    }

    @Test
    public void should_create_and_get_node(){
        JobFlow flow = new JobFlow();
        flow.setName("flow");
        flow.setPath("/flow");
        flow.setDuration((long) 10);
        JobStep step1 = new JobStep();
        step1.setParent(flow);
        step1.setName("step1");
        step1.setPath("/flow/step1");
        step1.setDuration((long) 10);
        flow.getChildren().add(step1);
        Node node = jobNodeService.create(flow);
        NodeUtil.recurse(flow, item->{
            Assert.assertEquals(((JobNode)item).getDuration(), ((JobNode)jobNodeService.find(flow.getPath())).getDuration());
        });
    }
}
