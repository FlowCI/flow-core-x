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

import com.flow.platform.api.domain.CmdQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
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
import com.flow.platform.domain.Jsonable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase {

    @Autowired
    private JobNodeService jobNodeService;

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_since_flow_status_is_not_ready() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        jobService.createJob(rootForFlow.getPath());
    }

    @Test
    public void should_create_node_success() throws IOException {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        Assert.assertNotNull(job.getId());
        Assert.assertEquals(NodeStatus.PENDING, job.getResult().getStatus());
        Step step1 = (Step) nodeService.find("flow1/step1");
        Step step2 = (Step) nodeService.find("flow1/step2");
        Step step3 = (Step) nodeService.find("flow1/step3");
        Flow flow = (Flow) nodeService.find(job.getNodePath());

        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);

        jobService.callback(new CmdQueueItem(job.getId(), cmd));

        job = jobService.find(job.getId());
        Assert.assertEquals("11111111", job.getSessionId());

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);
        cmd.setExtra(step1.getPath());

        jobService.callback(new CmdQueueItem(job.getId(), cmd));
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, job.getResult().getStatus());
        job = jobService.find(job.getId());
        NodeResult jobFlow = nodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setExtra(step2.getPath());

        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(1);
        cmdResult.setDuration(10l);
        cmd.setCmdResult(cmdResult);

        jobService.callback(new CmdQueueItem(job.getId(), cmd));
        job = jobService.find(job.getId());

        Assert.assertEquals(NodeStatus.FAILURE, (nodeResultService.find(step2.getPath(), job.getId())).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, job.getResult().getStatus());
        jobFlow = nodeResultService.find(flow.getPath(), job.getId());

        Assert.assertEquals(NodeStatus.FAILURE, jobFlow.getStatus());
    }

    @Test
    public void should_run_job_with_success_status() throws Throwable {
        // given:
        final String sessionId = "session-id-1";
        stubDemo();
        Node root = createRootFlow("flow-run-job", "for_job_service_run_job.yaml");
        setFlowToReady(root);

        // when: create job and job should be SESSION_CREATING
        Job job = jobService.createJob(root.getPath());
        Assert.assertNotNull(job);
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(JobStatus.SESSION_CREATING, job.getStatus());

        // when: simulate cc callback for create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId(sessionId);
        cmd.setStatus(CmdStatus.SENT);
        CmdQueueItem createSessionItem = new CmdQueueItem(job.getId(), cmd);
        jobService.callback(createSessionItem);

        // then: check job status should be running
        job = jobService.find(job.getId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(JobStatus.RUNNING, job.getStatus());

        // then: check job tree data is correct
        NodeTree tree = jobNodeService.get(job.getId());
        List<Node> steps = tree.ordered();
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

            CmdQueueItem runStepShellItem = new CmdQueueItem(job.getId(), stepCmd);
            jobService.callback(runStepShellItem);
            stepResult = nodeResultService.find(step.getPath(), job.getId());
            Assert.assertEquals(NodeStatus.SUCCESS, stepResult.getStatus());
        }

        job = jobService.find(job.getId());
        Assert.assertEquals(JobStatus.SUCCESS, job.getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, job.getResult().getStatus());
    }

    @Test
    public void should_stop_success() throws IOException {
        stubDemo();

        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());
        Assert.assertNotNull(job.getSessionId());

        Assert.assertEquals(true, jobService.stopJob(job.getNodeName(), job.getNumber()));
        job = jobService.find(job.getNodeName(), job.getNumber());
        Assert.assertEquals(NodeStatus.STOPPED, job.getResult().getStatus());
    }
}
