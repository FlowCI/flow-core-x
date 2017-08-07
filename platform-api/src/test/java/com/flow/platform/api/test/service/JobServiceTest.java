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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

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
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase {

    @Autowired
    private JobService jobService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobNodeService jobNodeService;

    private void stubDemo() {
        Cmd cmdRes = new Cmd();
        cmdRes.setId(UUID.randomUUID().toString());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));
    }

    @Test
    public void should_create_node_success() {
        stubDemo();

        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");
        Step step3 = new Step("step3", "/flow/step3");
        Step step4 = new Step("step4", "/flow/step4");
        Step step5 = new Step("step5", "/flow/step5");
        Step step6 = new Step("step6", "/flow/step6");
        Step step7 = new Step("step7", "/flow/step7");
        Step step8 = new Step("step8", "/flow/step8");

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
        Assert.assertEquals(NodeStatus.ENQUEUE, job.getStatus());
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(job.getId().toString(), cmd);

        job = jobService.find(job.getId());
        Assert.assertEquals("11111111", job.getSessionId());

        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);
        jobService.callback(step3.getPath(), cmd);
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, job.getStatus());
        job = jobService.find(job.getId());
        JobFlow jobFlow = (JobFlow) jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setType(CmdType.RUN_SHELL);
        jobService.callback(step2.getPath(), cmd);
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, ( jobNodeService.find(step2.getPath(), job.getId())).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, job.getStatus());
        jobFlow = (JobFlow) jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, jobFlow.getStatus());

    }

    @Test
    public void should_show_list_success() {
        stubDemo();

        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

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
        Job job = jobService.createJob(flow.getPath());
        List<JobStep> jobSteps = jobService.listJobStep(job.getId());
        Assert.assertEquals(2, jobSteps.size());
    }

}
