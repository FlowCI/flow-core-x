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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.git.model.GitEventType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class CmdWebhookControllerTest extends TestBase {

    @Before
    public void before() throws Throwable {
        stubDemo();
    }

    @Test
    public void should_callback_session_success() throws Throwable {
        // given: flow with two steps , step1 and step2
        Node rootForFlow = createRootFlow("flow1", "demo_flow.yaml");
        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.PR, null, mockUser);
        final String sessionId = "1111111";

        // when: create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        cmd.setSessionId(sessionId);

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: verify job status and root node status
        job = reload(job);
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, job.getRootResult().getStatus());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());
        Assert.assertEquals(GitEventType.PR, job.getCategory());

        Step step1 = (Step) nodeService.find("flow1/step1");
        Step step2 = (Step) nodeService.find("flow1/step2");

        // when: first step callback with running status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setExtra(step1.getPath());

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: verify node status
        job = reload(job);
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        NodeResult resultForStep1 = nodeResultService.find(step1.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForStep1.getStatus());

        NodeResult resultForRoot = nodeResultService.find(job.getNodePath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForRoot.getStatus());

        // when: first step callback with logged status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step1.getPath());

        CmdResult cmdResult = new CmdResult(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: verify job and node status
        job = reload(job);
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        resultForStep1 = nodeResultService.find(step1.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForStep1.getStatus());
        Assert.assertEquals(0, resultForStep1.getExitCode().intValue());

        resultForRoot = nodeResultService.find(job.getNodePath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForRoot.getStatus());

        // when: second step callback with logged status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step2.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step2.getPath());

        cmdResult = new CmdResult(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: verify job and node status
        job = reload(job);
        Assert.assertEquals(JobStatus.SUCCESS, job.getStatus());

        NodeResult resultForStep2 = nodeResultService.find(step2.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForStep2.getStatus());
        Assert.assertEquals(0, resultForStep2.getExitCode().intValue());

        resultForRoot = nodeResultService.find(job.getNodePath(), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForRoot.getStatus());
    }

    @Test
    public void should_on_callback_with_timeout() throws Throwable {
        // init
        Node rootForFlow = createRootFlow("flow1", "demo_flow.yaml");

        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.PR, null, mockUser);
        Step step2 = (Step) nodeService.find("flow1/step2");
        Step step1 = (Step) nodeService.find("flow1/step1");
        Flow flow = (Flow) nodeService.find(job.getNodePath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        job = jobService.find(job.getNodePath(), job.getNumber());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, job.getRootResult().getStatus());
        Assert.assertEquals(GitEventType.PR, job.getCategory());

        // when: first step with timeout status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        cmd.setExtra(step1.getPath());

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: verify job status
        job = jobService.find(job.getNodePath(), job.getNumber());
        Assert.assertEquals(JobStatus.FAILURE, job.getStatus());

        // then: verify first node result status
        NodeResult firstStepResult = nodeResultService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(firstStepResult.getCmdId());
        Assert.assertEquals(NodeStatus.TIMEOUT, firstStepResult.getStatus());

        // then: verify root result
        NodeResult rootResult = nodeResultService.find(job.getNodePath(), job.getId());
        Assert.assertEquals(job.getRootResult(), rootResult);
        Assert.assertEquals(NodeStatus.TIMEOUT, rootResult.getStatus());
    }

    @Test
    public void should_callback_with_timeout_but_allow_failure() throws Throwable {
        Node rootForFlow = createRootFlow("flow1", "demo_flow1.yaml");
        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.PR, null, mockUser);
        final String sessionId = "1111111";

        // when: create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        cmd.setSessionId(sessionId);

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: check job session id
        job = reload(job);
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, job.getRootResult().getStatus());

        Step step1 = (Step) nodeService.find("flow1/step1");

        // when: mock running status from agent
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setExtra(step1.getPath());

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: check root node result status should be RUNNING
        job = reload(job);
        Assert.assertEquals(NodeStatus.RUNNING, job.getRootResult().getStatus());

        // mock timeout kill status from agent
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        cmd.setExtra(step1.getPath());

        performMockHttpRequest(cmd, job);
        Thread.sleep(1000);

        // then: check step node status should be timeout
        NodeResult stepResult = nodeResultService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(stepResult.getCmdId());
        Assert.assertEquals(NodeStatus.TIMEOUT, stepResult.getStatus());

        // then: check root node status should be timeout as well
        NodeResult rootResult = nodeResultService.find(job.getNodePath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, rootResult.getStatus());

        // then: check job status should be running since time out allow failure
        job = reload(job);
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());
    }

    @Test
    public void should_on_callback_with_failure() {
        // TODO:
    }

    @Test
    public void should_on_callback_with_failure_but_allow_failure() {
        // TODO:
    }

    private Job reload(Job job) {
        return jobService.find(job.getNodePath(), job.getNumber());
    }

    private MvcResult performMockHttpRequest(Cmd cmd, Job job) throws Throwable {
        MockHttpServletRequestBuilder content = post(
            "/hooks/cmd?identifier=" + HttpUtil.urlEncode(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());

        return this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    }
}
