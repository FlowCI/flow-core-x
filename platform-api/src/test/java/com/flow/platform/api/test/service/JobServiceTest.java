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

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.job.JobNodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.queue.PlatformQueue;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase {

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdCallbackQueue;

    @Before
    public void init() {
        stubDemo();
    }

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_since_flow_status_is_not_ready() throws IOException {
        Node rootForFlow = nodeService.createEmptyFlow("flow1");
        jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.MANUAL, null, mockUser);
    }

    @Test
    public void should_job_failure_since_cannot_create_session() throws Throwable {
        // given: clean up all mock url to simulate create session cmd failure
        wireMockRule.resetAll();

        // when: create job
        Node rootForFlow = createRootFlow("flow1", "yml/demo_flow2.yaml");

        // mock latest commit for git service
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.MANUAL, null, mockUser);

        // then: verify job status
        job = jobService.find(job.getId());
        Assert.assertEquals(JobStatus.FAILURE, job.getStatus());
        Assert.assertNotNull(job.getFailureMessage());
        Assert.assertTrue(job.getFailureMessage().startsWith("Create session"));
    }

    @Test
    public void should_create_node_success() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "yml/demo_flow2.yaml");
        Job job = createMockJob(rootForFlow.getPath());
        Assert.assertNotNull(job.getEnv("FLOW_WORKSPACE"));
        Assert.assertNotNull(job.getEnv("FLOW_VERSION"));

        NodeTree nodeTree = nodeService.find("flow1");
        Node step1 = nodeTree.find("flow1/step1");
        Node step2 = nodeTree.find("flow1/step2");
        Node step3 = nodeTree.find("flow1/step3");
        Node flow = nodeTree.root();

        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        job = reload(job);
        Assert.assertEquals("11111111", job.getSessionId());
        Assert.assertEquals(JobCategory.TAG, job.getCategory());

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);
        cmd.setExtra(step1.getPath());

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        job = reload(job);
        Assert.assertEquals(NodeStatus.RUNNING, job.getRootResult().getStatus());

        job = reload(job);
        NodeResult jobFlow = nodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step2.getPath());

        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(1);
        cmdResult.setDuration(10L);
        cmd.setCmdResult(cmdResult);

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        job = reload(job);

        Assert.assertEquals(NodeStatus.FAILURE, (nodeResultService.find(step2.getPath(), job.getId())).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, job.getRootResult().getStatus());
        jobFlow = nodeResultService.find(flow.getPath(), job.getId());

        Assert.assertEquals(NodeStatus.FAILURE, jobFlow.getStatus());
    }

    @Test
    public void should_run_job_with_success_status() throws Throwable {
        // given:
        final String sessionId = "session-id-1";
        Node root = createRootFlow("flow_run_job", "yml/for_job_service_run_job.yaml");

        // when: create job and job should be SESSION_CREATING
        Job job = createMockJob(root.getPath());

        // then: check cmd request to create session
        verify(exactly(1), postRequestedFor(urlEqualTo("/cmd/queue/send?priority=1&retry=5")));

        // when: simulate cc callback for create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        CmdCallbackQueueItem createSessionItem = new CmdCallbackQueueItem(job.getId(), cmd);
        jobService.callback(createSessionItem);
        verify(exactly(1), postRequestedFor(urlEqualTo("/cmd/send")));

        // then: check job status should be running
        job = reload(job);
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        // then: check job tree data is correct
        NodeTree tree = jobNodeService.get(job);
        List<Node> steps = tree.children();
        Assert.assertEquals(7, steps.size());

        // when: simulate callback for all steps
        int numOfRequestForCmdSend = 2;
        for (Node step : steps) {
            NodeResult stepResult = nodeResultService.find(step.getPath(), job.getId());

            // check step root status should be success
            if (!tree.canRun(step.getPath())) {
                Assert.assertEquals(NodeStatus.SUCCESS, stepResult.getStatus());
                Assert.assertEquals(60L, stepResult.getDuration().longValue());
                continue;
            }

            Assert.assertEquals(NodeStatus.PENDING, stepResult.getStatus());

            // simulate callback with success executed
            Cmd stepCmd = new Cmd("default", null, CmdType.RUN_SHELL, step.getScript());
            stepCmd.setSessionId(sessionId);
            stepCmd.setStatus(CmdStatus.LOGGED);
            stepCmd.setCmdResult(new CmdResult(0));
            stepCmd.setExtra(step.getPath());

            // set start and finish time for 30 seconds of every steps
            ZonedDateTime start = ZonedDateTime.now();
            ZonedDateTime finish = start.plusSeconds(30);
            stepCmd.getCmdResult().setStartTime(start);
            stepCmd.getCmdResult().setFinishTime(finish);

            // build mock identifier
            CmdCallbackQueueItem runStepShellItem = new CmdCallbackQueueItem(job.getId(), stepCmd);
            jobService.callback(runStepShellItem);
            stepResult = nodeResultService.find(step.getPath(), job.getId());
            Assert.assertEquals(NodeStatus.SUCCESS, stepResult.getStatus());

            verify(exactly(numOfRequestForCmdSend++), postRequestedFor(urlEqualTo("/cmd/send")));

            verify(moreThanOrExactly(1),
                postRequestedFor(urlEqualTo("/cmd/send"))
                    .withRequestBody(matchingJsonPath("$.inputs[?(@.FLOW_JOB_LAST_STATUS == 'SUCCESS')]")));
        }

        // then: check num of cmd request to run shell, 5 steps + 1 del session requests
        verify(exactly(6), postRequestedFor(urlEqualTo("/cmd/send")));

        // then: check job status
        job = reload(job);
        Assert.assertEquals(JobStatus.SUCCESS, job.getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, job.getRootResult().getStatus());
    }

    @Test
    public void should_stop_success() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "yml/demo_flow2.yaml");
        Job job = createMockJob(rootForFlow.getPath());

        Job stoppedJob = jobService.stop(job.getNodeName(), job.getNumber());
        Assert.assertNotNull(stoppedJob);
        stoppedJob = jobService.find(stoppedJob.getId());
        Assert.assertEquals(NodeStatus.STOPPED, stoppedJob.getRootResult().getStatus());
    }

    @Test
    public void should_stop_running_job_success() throws IOException {

        // init flow
        Node rootForFlow = createRootFlow("flow1", "yml/demo_flow2.yaml");
        NodeTree nodeTree = nodeService.find("flow1");
        Node stepFirst = nodeTree.find("flow1/step1");

        // create job
        Job job = createMockJob(rootForFlow.getPath());

        // mock callback cmd
        final String sessionId = CommonUtil.randomId().toString();
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));
        job = reload(job);

        // first step should running
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, stepFirst.getScript());
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);
        cmd.setExtra(stepFirst.getPath());
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // job should running
        job = reload(job);
        Assert.assertEquals(NodeStatus.RUNNING, job.getRootResult().getStatus());

        // job should stop
        Job stoppedJob = jobService.stop(job.getNodeName(), job.getNumber());
        Assert.assertNotNull(stoppedJob);
        stoppedJob = jobService.find(stoppedJob.getId());
        Assert.assertEquals(NodeStatus.STOPPED, stoppedJob.getRootResult().getStatus());
    }

    @Test
    public void should_job_time_out_and_reject_callback() throws IOException, InterruptedException {
        // given: job and mock updated time as expired
        Node rootForFlow = createRootFlow("flow1", "yml/demo_flow2.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);
        Assert.assertNotNull(job.getEnv("FLOW_WORKSPACE"));
        Assert.assertNotNull(job.getEnv("FLOW_VERSION"));

        // when: check job timeout
        ThreadUtil.sleep(20000);
        jobService.checkTimeOut(job);

        // then: job status should be timeout
        Job jobRes = jobDao.get(rootForFlow.getPath(), job.getNumber());
        Assert.assertEquals(JobStatus.TIMEOUT, jobRes.getStatus());
        Assert.assertEquals(NodeStatus.TIMEOUT, jobRes.getRootResult().getStatus());

        // when: mock some callback for job
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("xxxx");
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        // then:
        Assert.assertEquals(JobStatus.TIMEOUT, jobService.find(job.getId()).getStatus());
    }

    @Test
    public void should_get_latest_job_by_node_path() throws IOException {
        Node rootForFlow = createRootFlow("flowTest", "yml/demo_flow1.yaml");
        createMockJob(rootForFlow.getPath());
        createMockJob(rootForFlow.getPath());

        Assert.assertEquals(2, jobDao.list().size());

        List<String> rootPath = Lists.newArrayList(rootForFlow.getPath());
        List<Job> jobs = jobService.list(rootPath, true);
        Assert.assertEquals(1, jobs.size());
        Assert.assertEquals("2", jobs.get(0).getNumber().toString());
    }

    @Test
    public void should_cmd_enqueue_within_limit_times() throws InterruptedException {
        // init:
        Cmd cmd = new Cmd("default", "test", CmdType.RUN_SHELL, "echo 1");
        CountDownLatch countDownLatch = new CountDownLatch(5);
        AtomicInteger atomicInteger = new AtomicInteger(0);
        cmdCallbackQueue.clean();

        // given: register new queue listener to get item info
        cmdCallbackQueue.register(message -> {
            CmdCallbackQueueItem item = CmdCallbackQueueItem.parse(message.getBody(), CmdCallbackQueueItem.class);
            atomicInteger.set(item.getRetryTimes());
            countDownLatch.countDown();
        });

        // when: enter queue one not found job id
        jobService.enqueue(new CmdCallbackQueueItem(CommonUtil.randomId(), cmd), 1);
        boolean await = countDownLatch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(await);

        // then: should try 5 times
        Assert.assertEquals(1, atomicInteger.get());

        // then: cmdCallbackQueue size should 0
        Assert.assertNull(cmdCallbackQueue.dequeue());
        Assert.assertEquals(0, cmdCallbackQueue.size());
    }
}
