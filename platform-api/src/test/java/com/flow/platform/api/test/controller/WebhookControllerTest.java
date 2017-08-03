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

package com.flow.platform.api.test.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.Logger;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class WebhookControllerTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private JobService jobService;

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
    public void should_callback_session_success() throws Exception {

        stubDemo();

        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);

        nodeService.create(flow);

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        // run first step running
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step1.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        JobStep jobStep1 = (JobStep) jobNodeService.find(step1.getPath());
        JobFlow jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);

        // run first step finish
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step1.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        jobStep1 = (JobStep) jobNodeService.find(step1.getPath());
        jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep1.getExitCode());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);

        // run first step finish
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmdResult = new CmdResult();
        cmdResult.setExitValue(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step2.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        JobStep jobStep2 = (JobStep) jobNodeService.find(step2.getPath());
        jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(jobStep2.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep2.getExitCode());
        Assert.assertEquals(job.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.SUCCESS);
    }

    @Test
    public void should_callback_failure() throws Exception {

        stubDemo();

        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);

        nodeService.create(flow);

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step1.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        JobStep jobStep1 = (JobStep) jobNodeService.find(step1.getPath());
        Assert.assertNotNull(jobStep1.getCmdId());
        JobFlow jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        Assert.assertEquals(job.getStatus(), NodeStatus.FAILURE);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.TIMEOUT);
    }

    @Test
    public void should_callback_timeout_allow_failure() throws Exception {

        stubDemo();

        Flow flow = new Flow("flow", "/flow");

        Step step1 = new Step("step1", "/flow/step1");
        Step step2 = new Step("step2", "/flow/step2");

        step1.setAllowFailure(true);

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);

        nodeService.create(flow);

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step1.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(step1.getPath()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        JobStep jobStep1 = (JobStep) jobNodeService.find(step1.getPath());
        Assert.assertNotNull(jobStep1.getCmdId());
        JobFlow jobFlow = (JobFlow) jobNodeService.find(flow.getPath());
        job = jobService.find(job.getId());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);
    }
}
