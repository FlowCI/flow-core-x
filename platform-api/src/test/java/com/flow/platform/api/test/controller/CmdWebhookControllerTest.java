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

import com.flow.platform.api.consumer.JobStatusEventConsumer;
import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.events.JobStatusChangeEvent;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.queue.PlatformQueue;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class CmdWebhookControllerTest extends TestBase {

    private final static String sessionId = "1111111";

    @Autowired
    private PlatformQueue<PriorityMessage> cmdCallbackQueue;

    @Autowired
    private SpringContext springContext;

    @Before
    public void before() throws Throwable {
        stubSendCmdToQueue(sessionId);
        stubSendCmd(sessionId);

        cmdCallbackQueue.clean();
        springContext.cleanApplictionListener();
    }

    @Test
    public void should_callback_session_success() throws Throwable {
        // given: flow with two steps , step1 and step2
        final Node rootForFlow = createRootFlow("flow1", "yml/demo_flow.yaml");
        final Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.PR, null, mockUser);
        final CountDownLatch runningLatch = createCountDownForJobStatusChange(job, JobStatus.RUNNING, 1);

        // when: create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setId(UUID.randomUUID().toString());
        cmd.setStatus(CmdStatus.SENT);
        cmd.setSessionId(sessionId);

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        Assert.assertTrue(runningLatch.await(10, TimeUnit.SECONDS));

        // then: verify job status and root node status
        Job reloaded = refresh(job);
        Assert.assertEquals(sessionId, reloaded.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, reloaded.getRootResult().getStatus());
        Assert.assertEquals(JobStatus.RUNNING, reloaded.getStatus());
        Assert.assertEquals(JobCategory.PR, reloaded.getCategory());

        NodeTree nodeTree = nodeService.find("flow1");
        Node step1 = nodeTree.find("flow1/step1");
        Node step2 = nodeTree.find("flow1/step2");

        // when: first step callback with running status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setExtra(step1.getPath());
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: verify node status
        reloaded = refresh(reloaded);
        Assert.assertEquals(JobStatus.RUNNING, reloaded.getStatus());

        NodeResult resultForStep1 = nodeResultService.find(step1.getPath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForStep1.getStatus());

        NodeResult resultForRoot = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForRoot.getStatus());

        // when: first step callback with logged status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step1.getPath());

        CmdResult cmdResult = new CmdResult(0);
        cmdResult.setDuration(100L);
        cmdResult.setOutput(EnvUtil.build("OUTPUT_ENV", "hello"));
        cmd.setCmdResult(cmdResult);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: verify job and node status
        reloaded = refresh(job);
        Assert.assertEquals(JobStatus.RUNNING, reloaded.getStatus());

        // check step 1 node result
        resultForStep1 = nodeResultService.find(step1.getPath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForStep1.getStatus());
        Assert.assertEquals(0, resultForStep1.getExitCode().intValue());
        Assert.assertEquals(1, resultForStep1.getOutputs().size());
        Assert.assertEquals("hello", resultForStep1.getOutputs().get("OUTPUT_ENV"));

        // check root node result
        NodeResult rootNodeResult = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(2, rootNodeResult.getOutputs().size());
        Assert.assertNotNull(rootNodeResult.getOutputs().get("FLOW_JOB_LOG_PATH"));
        Assert.assertEquals("hello", rootNodeResult.getOutputs().get("OUTPUT_ENV"));

        resultForRoot = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.RUNNING, resultForRoot.getStatus());

        // when: second step callback with logged status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step2.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step2.getPath());

        cmdResult = new CmdResult(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: verify job and node status
        reloaded = refresh(reloaded);
        Assert.assertEquals(JobStatus.SUCCESS, reloaded.getStatus());

        NodeResult resultForStep2 = nodeResultService.find(step2.getPath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForStep2.getStatus());
        Assert.assertEquals(0, resultForStep2.getExitCode().intValue());

        resultForRoot = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, resultForRoot.getStatus());
    }

    @Test
    public void should_callback_with_timeout() throws Throwable {
        // given: job and listener
        final Node rootForFlow = createRootFlow("flow1", "yml/demo_flow.yaml");
        final Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.PR, null, mockUser);

        NodeTree nodeTree = nodeService.find("flow1");
        Node step2 = nodeTree.find("flow1/step2");
        Node step1 = nodeTree.find("flow1/step1");
        Node flow = nodeTree.root();

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        cmd.setSessionId(sessionId);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        Job reloaded = refresh(job);
        Assert.assertNotNull(reloaded.getSessionId());
        Assert.assertEquals(sessionId, reloaded.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, reloaded.getRootResult().getStatus());
        Assert.assertEquals(JobCategory.PR, reloaded.getCategory());
        Assert.assertEquals(JobStatus.RUNNING, reloaded.getStatus());

        // when: first step with timeout status
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        cmd.setExtra(step1.getPath());
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: verify job status
        reloaded = refresh(reloaded);
        Assert.assertEquals(JobStatus.FAILURE, reloaded.getStatus());

        // then: verify first node result status
        NodeResult firstStepResult = nodeResultService.find(step1.getPath(), reloaded.getId());
        Assert.assertNotNull(firstStepResult.getCmdId());
        Assert.assertEquals(NodeStatus.TIMEOUT, firstStepResult.getStatus());

        // then: verify root result
        NodeResult rootResult = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(reloaded.getRootResult(), rootResult);
        Assert.assertEquals(NodeStatus.TIMEOUT, rootResult.getStatus());
    }

    @Test
    public void should_callback_with_timeout_but_allow_failure() throws Throwable {
        final Node rootForFlow = createRootFlow("flow1", "yml/demo_flow1.yaml");
        final Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.PR, null, mockUser);

        // when: create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        cmd.setSessionId(sessionId);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: check job session id
        Job reloaded = refresh(job);
        Assert.assertEquals(sessionId, reloaded.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, reloaded.getRootResult().getStatus());

        Node step1 = nodeService.find("flow1").find("flow1/step1");

        // when: mock running status from agent
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setExtra(step1.getPath());
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: check root node result status should be RUNNING
        reloaded = refresh(reloaded);
        Assert.assertEquals(NodeStatus.RUNNING, reloaded.getRootResult().getStatus());

        // mock timeout kill status from agent
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        cmd.setExtra(step1.getPath());
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then: check step node status should be timeout
        NodeResult stepResult = nodeResultService.find(step1.getPath(), reloaded.getId());
        Assert.assertNotNull(stepResult.getCmdId());
        Assert.assertEquals(NodeStatus.TIMEOUT, stepResult.getStatus());

        // then: check root node status should be timeout as well
        NodeResult rootResult = nodeResultService.find(reloaded.getNodePath(), reloaded.getId());
        Assert.assertEquals(NodeStatus.RUNNING, rootResult.getStatus());

        // then: check job status should be running since time out allow failure
        reloaded = refresh(reloaded);
        Assert.assertEquals(JobStatus.RUNNING, reloaded.getStatus());
    }

    @Test
    public void should_on_callback_with_failure() {
        // TODO:
    }

    @Test
    public void should_on_callback_with_failure_but_allow_failure() {
        // TODO:
    }

    protected Job refresh(Job job) {
        return jobService.find(job.getId());
    }

    private void stubSendCmdToQueue(String sessionId) {
        Cmd mockSessionCmd = new Cmd();
        mockSessionCmd.setSessionId(sessionId);
        mockSessionCmd.setId(UUID.randomUUID().toString());

        stubFor(WireMock.post(urlEqualTo("/cmd/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(mockSessionCmd.toJson())));
    }

    private void stubSendCmd(String sessionId) {
        Cmd sendCmd = new Cmd();
        sendCmd.setSessionId(sessionId);
        sendCmd.setId(UUID.randomUUID().toString());

        stubFor(WireMock.post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse()
                .withBody(sendCmd.toJson())));
    }

    private CountDownLatch createCountDownForJobStatusChange(Job job, JobStatus expect, int num) {
        final CountDownLatch countDown = new CountDownLatch(num);
        springContext.registerApplicationListener(new JobStatusEventConsumer() {
            @Override
            public void onApplicationEvent(JobStatusChangeEvent event) {
                if (event.getJob().equals(job) && event.getTo() == expect) {
                    countDown.countDown();
                }
            }
        });
        return countDown;
    }
}
