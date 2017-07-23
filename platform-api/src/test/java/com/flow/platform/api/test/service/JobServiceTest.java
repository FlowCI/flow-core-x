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
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ObjectUtil;
import com.sun.org.apache.regexp.internal.RE;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase{
    @Autowired
    JobService jobService;

    @Autowired
    NodeService nodeService;

    @Autowired
    JobNodeService jobNodeService;

    @Test
    public void should_copy_node(){
        Flow flow = new Flow();
        flow.setPath("/flow");
        flow.setName("flow");
        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/flow/step1");
        step1.setPlugin("step1");
        step1.setAllowFailure(true);
        Step step2 = new Step();
        step2.setName("step2");
        step2.setPath("/flow/step2");
        step2.setPlugin("step2");
        step2.setAllowFailure(true);
        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setParent(step1);
        nodeService.create(flow);

        JobFlow jobFlow = jobService.createJobNode(flow.getPath());
        jobFlow.getChildren().forEach(item -> {
            Assert.assertEquals(item.getPath(), jobNodeService.find(item.getPath()).getPath());
        });
    }

    @Test
    public void should_copy_node_simple(){
        // zero node

        Flow flow = new Flow();
        flow.setPath("/flow");
        flow.setName("flow");

        nodeService.create(flow);

        JobFlow jobFlow = jobService.createJobNode(flow.getPath());

        jobFlow = (JobFlow) jobNodeService.find(jobFlow.getPath());
        jobFlow.getChildren().forEach(item -> {
            Assert.assertEquals(item.getPath(), jobNodeService.find(item.getPath()).getPath());
        });
    }

    @Test
    public void should_create_node_success(){
        Flow flow = new Flow();
        flow.setName("flow");
        flow.setPath("/flow");
        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/flow/step1");
        Step step2 = new Step();
        step2.setName("step2");
        step2.setPath("/flow/step2");
        Step step3 = new Step();
        step3.setName("step3");
        step3.setPath("/flow/step3");
        Step step4 = new Step();
        step4.setName("step4");
        step4.setPath("/flow/step4");
        Step step5 = new Step();
        step5.setName("step5");
        step5.setPath("/flow/step5");
        Step step6 = new Step();
        step6.setName("step6");
        step6.setPath("/flow/step6");
        Step step7 = new Step();
        step7.setName("step7");
        step7.setPath("/flow/step7");
        Step step8 = new Step();
        step8.setName("step8");
        step8.setPath("/flow/step8");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);

        step1.getChildren().add(step3);
        step1.getChildren().add(step4);
        step3.setParent(step1);
        step4.setParent(step1);

        step2.getChildren().add(step5);
        step2.getChildren().add(step6);
        step5.setParent(step2);
        step6.setParent(step2);

        step4.getChildren().add(step7);
        step4.getChildren().add(step8);
        step8.setParent(step4);
        step7.setParent(step4);

        nodeService.create(flow);
        Job job = jobService.createJob(flow.getPath());
        Assert.assertNotNull(job.getId());
        Assert.assertEquals(NodeStatus.PENDING, job.getStatus());
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(job.getId(), cmd);
        Assert.assertEquals("11111111", job.getSessionId());

        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);
        jobService.callback(step3.getPath(), cmd);
        Assert.assertEquals(NodeStatus.RUNNING, job.getStatus());
        JobFlow jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setType(CmdType.RUN_SHELL);
        jobService.callback(step2.getPath(), cmd);
        Assert.assertEquals(NodeStatus.SUCCESS, ((JobStep)jobNodeService.find(step2.getPath())).getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, job.getStatus());
        jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(NodeStatus.SUCCESS, jobFlow.getStatus());

    }

}
