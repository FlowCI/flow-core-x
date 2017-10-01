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

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.envs.JobEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.service.job.JobNodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.git.model.GitEventType;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
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

    @Before
    public void init() {
        stubDemo();
    }

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_since_flow_status_is_not_ready() throws IOException {
        Flow rootForFlow = nodeService.createEmptyFlow("flow1");
        jobService.createJob(rootForFlow.getPath(), GitEventType.MANUAL, null, mockUser);
    }

    @Test
    public void should_create_node_success() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        Job job = createMockJob(rootForFlow.getPath());

        Step step1 = (Step) nodeService.find("flow1/step1");
        Step step2 = (Step) nodeService.find("flow1/step2");
        Step step3 = (Step) nodeService.find("flow1/step3");
        Flow flow = (Flow) nodeService.find(job.getNodePath());

        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);

        jobService.callback(new CmdCallbackQueueItem(job.getId(), cmd));

        job = reload(job);
        Assert.assertEquals("11111111", job.getSessionId());
        Assert.assertEquals(GitEventType.TAG, job.getCategory());

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
        Node root = createRootFlow("flow-run-job", "for_job_service_run_job.yaml");

        // when: create job and job should be SESSION_CREATING
        Job job = createMockJob(root.getPath());

        // then: check cmd request to create session
        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(countStrategy, postRequestedFor(urlEqualTo("/cmd/queue/send?priority=1&retry=5")));

        // when: simulate cc callback for create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        CmdCallbackQueueItem createSessionItem = new CmdCallbackQueueItem(job.getId(), cmd);
        jobService.callback(createSessionItem);

        // then: check job status should be running
        job = reload(job);
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        // then: check job tree data is correct
        NodeTree tree = jobNodeService.get(job);
        List<Node> steps = tree.children();
        Assert.assertEquals(7, steps.size());

        // when: simulate callback for all steps
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
        }

        // then: check num of cmd request to run shell, 5 steps + 1 del session requests
        countStrategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 6);
        verify(countStrategy, postRequestedFor(urlEqualTo("/cmd/send")));

        // then: check job status
        job = reload(job);
        Assert.assertEquals(JobStatus.SUCCESS, job.getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, job.getRootResult().getStatus());
    }

    @Test
    public void should_stop_success() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        Job job = createMockJob(rootForFlow.getPath());

        Job stoppedJob = jobService.stopJob(job.getNodeName(), job.getNumber());
        Assert.assertNotNull(stoppedJob);
        Assert.assertEquals(NodeStatus.STOPPED, stoppedJob.getRootResult().getStatus());
    }

    @Test
    public void should_job_time_out_and_reject_callback() throws IOException, InterruptedException {
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.TAG, null, mockUser);
        Thread.sleep(7000);

        // when: check job timeout
        jobService.checkTimeoutTask();

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
    public void getLatestByPath() throws IOException{
        Node rootForFlow = createRootFlow("flowTest", "demo_flow1.yaml");
        createMockJob(rootForFlow.getPath());
        createMockJob(rootForFlow.getPath());

        Assert.assertEquals(2, jobDao.list().size());

        List<String> rootPath = Lists.newArrayList(rootForFlow.getPath());
        List<Job> jobs = jobService.list(rootPath,true);
        Assert.assertEquals(1, jobs.size());
        Assert.assertEquals("2", jobs.get(0).getNumber().toString());

    }

    private Job createMockJob(String nodePath) {
        Job job = jobService.createJob(nodePath, GitEventType.TAG, null, mockUser);
        Assert.assertNotNull(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getNumber());
        Assert.assertEquals(mockUser.getEmail(), job.getCreatedBy());
        Assert.assertEquals(JobStatus.SESSION_CREATING, job.getStatus());

        Assert.assertEquals(job.getNumber().toString(), job.getEnv(JobEnvs.FLOW_JOB_BUILD_NUMBER));

        // verify root node result for job
        NodeResult rootResult = job.getRootResult();
        Assert.assertNotNull(rootResult);
        Assert.assertEquals(NodeTag.FLOW, rootResult.getNodeTag());
        Assert.assertNotNull(rootResult.getOutputs());
        Assert.assertEquals(NodeStatus.PENDING, rootResult.getStatus());

        NodeTree nodeTree = jobNodeService.get(job);

        // verify child node result list
        List<NodeResult> childrenResult = job.getChildrenResult();
        Assert.assertNotNull(childrenResult);
        Assert.assertEquals(nodeTree.childrenSize(), childrenResult.size());

        return job;
    }

    private Job reload(Job job) {
        return jobService.find(job.getNodePath(), job.getNumber());
    }
}
