/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.job;

import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.service.JobEventService;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.*;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@Log4j2
@FixMethodOrder(MethodSorters.JVM)
public class JobServiceTest extends ZookeeperScenario {

    @Autowired
    private JobDao jobDao;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private JobEventService jobEventService;

    @Autowired
    private JobService jobService;

    @Autowired
    private StepService stepService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    private Flow flow;

    private Yml yml;

    @Before
    public void mockFlowAndYml() throws IOException {
        mockLogin();

        flow = flowService.create("hello");
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow.yml")));

        Assert.assertNotNull(flowJobQueueManager.get(flow.getQueueName()));
    }

    @Test
    public void should_create_job_with_expected_context() {
        // init:
        flow.getLocally().put("LOCAL_VAR", VarValue.of("local", VarType.STRING));
        flowService.update(flow);
        flow = flowService.get(flow.getName());

        StringVars input = new StringVars();
        input.put("INPUT_VAR", "input");

        // when: create job
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, input);
        Vars<String> context = job.getContext();
        Assert.assertNotNull(context);

        // then: default vars
        Assert.assertTrue(context.containsKey(Variables.Flow.Name));
        Assert.assertTrue(context.containsKey(Variables.App.Url));

        Assert.assertTrue(context.containsKey(Variables.Job.Status));
        Assert.assertTrue(context.containsKey(Variables.Job.BuildNumber));
        Assert.assertTrue(context.containsKey(Variables.Job.Trigger));
        Assert.assertTrue(context.containsKey(Variables.Job.TriggerBy));
        Assert.assertTrue(context.containsKey(Variables.Job.StartAt));
        Assert.assertTrue(context.containsKey(Variables.Job.FinishAt));

        // then: vars should be included in job context
        Assert.assertTrue(context.containsKey("LOCAL_VAR"));
        Assert.assertTrue(context.containsKey("INPUT_VAR"));
        Assert.assertTrue(context.containsKey("FLOW_WORKSPACE"));
        Assert.assertTrue(context.containsKey("FLOW_VERSION"));
    }

    @Test
    public void should_init_steps_cmd_after_job_created() {
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        List<ExecutedCmd> steps = stepService.list(job);
        Assert.assertNotNull(steps);
        Assert.assertEquals(2, steps.size());

        for (ExecutedCmd step : steps) {
            Assert.assertNotNull(step.getFlowId());
            Assert.assertNotNull(step.getCmdId());
        }
    }

    @Test
    public void should_start_new_job() throws Throwable {
        ObjectWrapper<Job> receivedJob = new ObjectWrapper<>();

        // init: register JobReceivedEvent
        CountDownLatch waitForJobFromQueue = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobReceivedEvent>) event -> {
            receivedJob.setValue(event.getJob());
            waitForJobFromQueue.countDown();
        });

        // when: create and start job
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);
        NodeTree tree = ymlManager.getTree(job);

        Assert.assertEquals(Status.CREATED, job.getStatus());
        Assert.assertEquals(tree.getRoot().getPath(), NodePath.create(job.getCurrentPath()));

        job = jobService.start(job);
        Assert.assertEquals(Status.QUEUED, job.getStatus());

        Assert.assertNotNull(job);
        Assert.assertNotNull(ymlManager.getTree(job));

        // then: confirm job is received from queue
        waitForJobFromQueue.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, waitForJobFromQueue.getCount());
        Assert.assertEquals(job, receivedJob.getValue());
    }

    @Test
    public void should_get_job_expire() {
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);
        Assert.assertFalse(jobService.isExpired(job));
    }

    @Test
    public void should_dispatch_job_to_agent() throws InterruptedException {
        // init:
        Agent agent = agentService.create("hello.agent", null, Optional.empty());
        mockAgentOnline(agentService.getPath(agent));

        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        // when:
        ObjectWrapper<Agent> targetAgent = new ObjectWrapper<>();
        ObjectWrapper<CmdIn> targetCmd = new ObjectWrapper<>();
        CountDownLatch counter = new CountDownLatch(1);

        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            targetAgent.setValue(event.getAgent());
            targetCmd.setValue(event.getCmd());
            counter.countDown();
        });

        jobService.start(job);

        // then: verify cmd been sent
        Assert.assertTrue(counter.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(agent, targetAgent.getValue());

        // then: verify job status should be running
        Assert.assertEquals(Status.RUNNING, jobDao.findById(job.getId()).get().getStatus());

        // then: verify cmd content
        Node root = YmlParser.load(flow.getName(), yml.getRaw());
        NodeTree tree = NodeTree.create(root);
        Node first = tree.next(tree.getRoot().getPath());

        CmdIn cmd = targetCmd.getValue();
        Assert.assertEquals(cmdManager.createId(job, first).toString(), cmd.getId());
        Assert.assertEquals("echo step version", cmd.getInputs().get("FLOW_VERSION"));
        Assert.assertEquals("echo step", cmd.getInputs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo hello\n", cmd.getScripts().get(0));
    }

    @Test
    public void should_handle_cmd_callback_for_success_status() {
        // init: agent and job
        Agent agent = agentService.create("hello.agent", null, Optional.empty());
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: cmd of first node been executed
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        ExecutedCmd executedCmd = new ExecutedCmd(
                cmdManager.createId(job, firstNode),
                job.getFlowId(),
                firstNode.isAllowFailure()
        );
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmd.setOutput(output);
        executedCmd.setBuildNumber(1L);

        jobEventService.handleCallback(executedCmd);

        // then: executed cmd should be saved
        ExecutedCmd saved = executedCmdDao.findById(executedCmd.getId()).get();
        Assert.assertNotNull(saved);
        Assert.assertEquals(executedCmd, saved);

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // then: job current context should be updated
        Node secondNode = tree.next(firstNode.getPath());
        Assert.assertEquals(secondNode.getPath(), NodePath.create(job.getCurrentPath()));

        // when: cmd of second node been executed
        output = new StringVars();
        output.put("HELLO_JAVA", "hello.java");

        executedCmd = new ExecutedCmd(
                cmdManager.createId(job, secondNode),
                job.getFlowId(),
                secondNode.isAllowFailure()
        );
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmd.setOutput(output);
        executedCmd.setBuildNumber(1L);

        jobEventService.handleCallback(executedCmd);

        // then: executed cmd of second node should be saved
        saved = executedCmdDao.findById(executedCmd.getId()).get();
        Assert.assertNotNull(saved);
        Assert.assertEquals(executedCmd, saved);

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals("hello.java", job.getContext().get("HELLO_JAVA"));
        Assert.assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));
        Assert.assertEquals(Status.SUCCESS, job.getStatus());
    }

    @Test
    public void should_handle_cmd_callback_for_failure_status_but_allow_failure() throws IOException {
        // init: agent and job
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-all-failure.yml")));
        Agent agent = agentService.create("hello.agent", null, Optional.empty());
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: cmd of first node with failure
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        ExecutedCmd executedCmd = new ExecutedCmd(
                cmdManager.createId(job, firstNode),
                job.getFlowId(),
                firstNode.isAllowFailure()
        );
        executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
        executedCmd.setOutput(output);

        jobEventService.handleCallback(executedCmd);

        // then: executed cmd should be recorded
        Assert.assertNotNull(executedCmdDao.findById(executedCmd.getId()).get());

        // then: job status should be running and current path should be change to second node
        job = jobDao.findById(job.getId()).get();
        Node secondNode = tree.next(firstNode.getPath());

        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(secondNode.getPathAsString(), job.getCurrentPath());
        Assert.assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // when: second cmd of node been timeout
        output = new StringVars();
        output.put("HELLO_TIMEOUT", "hello.timeout");

        executedCmd = new ExecutedCmd(
                cmdManager.createId(job, secondNode),
                job.getFlowId(),
                secondNode.isAllowFailure()
        );
        executedCmd.setStatus(ExecutedCmd.Status.TIMEOUT);
        executedCmd.setOutput(output);

        jobEventService.handleCallback(executedCmd);

        // then: executed cmd of second node should be recorded
        Assert.assertNotNull(executedCmdDao.findById(executedCmd.getId()).get());

        // then: job should be timeout with error message
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.SUCCESS, job.getStatus());
        Assert.assertEquals("hello.timeout", job.getContext().get("HELLO_TIMEOUT"));
    }

    @Test
    public void should_job_failure_with_final_node() throws Exception {
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-failure-with-final.yml")));
        Agent agent = agentService.create("hello.agent.0", null, Optional.empty());
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: set first step as failure status
        CmdId cmdId = cmdManager.createId(job, firstNode);
        ExecutedCmd executedCmd = new ExecutedCmd(cmdId, job.getFlowId(), firstNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
        jobEventService.handleCallback(executedCmd);

        // when: set final node as success status
        Node secondNode = tree.next(firstNode.getPath());
        cmdId = cmdManager.createId(job, secondNode);
        executedCmd = new ExecutedCmd(cmdId, job.getFlowId(), secondNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        jobEventService.handleCallback(executedCmd);

        // then: job status should be failure since final node does not count to step
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.FAILURE, job.getStatus());
    }

    @Test
    public void should_run_before_condition() throws IOException, InterruptedException {
        // init: save yml, make agent online and create job
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-with-before.yml")));

        Agent agent = agentService.create("hello.agent.1", null, Optional.empty());
        mockAgentOnline(agentService.getPath(agent));

        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        // init: wait counter
        CountDownLatch waitForJobQueued = new CountDownLatch(2);
        addEventListener((ApplicationListener<JobStatusChangeEvent>) event -> {
            if (event.getJob().getStatus() == Status.QUEUED) {
                waitForJobQueued.countDown();
            }

            if (event.getJob().getStatus() == Status.RUNNING) {
                waitForJobQueued.countDown();
            }
        });

        CountDownLatch waitForStep2Sent = new CountDownLatch(1);
        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            waitForStep2Sent.countDown();
        });

        // when:
        jobService.start(job);
        Assert.assertTrue(waitForJobQueued.await(10, TimeUnit.SECONDS));
        Assert.assertTrue(waitForStep2Sent.await(10, TimeUnit.SECONDS));

        // then: job should failure since script return false
        Job executed = jobDao.findById(job.getId()).get();
        List<ExecutedCmd> steps = stepService.list(executed);

        Assert.assertEquals(Status.RUNNING, executed.getStatus());
        Assert.assertEquals("hello/step2", executed.getCurrentPath());

        ExecutedCmd executedCmd = steps.get(0);
        Assert.assertEquals(ExecutedCmd.Status.SKIPPED, executedCmd.getStatus());
    }

    @Test
    public void should_cancel_job_if_agent_offline() throws IOException, InterruptedException {
        // init:
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-with-before.yml")));
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        // mock agent online
        Agent agent = agentService.create("hello.agent.2", null, Optional.empty());
        mockAgentOnline(agentService.getPath(agent));

        // given: start job and wait for running
        jobService.start(job);

        CountDownLatch waitForRunning = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobStatusChangeEvent>) event -> {
            if (event.getJob().getStatus() == Status.RUNNING) {
                waitForRunning.countDown();
            }
        });

        waitForRunning.await(10, TimeUnit.SECONDS);
        job = jobDao.findByKey(job.getKey()).get();
        Assert.assertEquals(Status.RUNNING, job.getStatus());

        // when: agent status change to offline
        CountDownLatch waitForCancelled = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobStatusChangeEvent>) event -> {
            if (event.getJob().getStatus() == Status.CANCELLED) {
                waitForCancelled.countDown();
            }
        });

        agent.setJobId(job.getId());
        agent.setStatus(Agent.Status.OFFLINE);
        multicastEvent(new AgentStatusEvent(this, agent));

        // then: job should be cancelled
        waitForCancelled.await();
        job = jobDao.findByKey(job.getKey()).get();
        Assert.assertEquals(Status.CANCELLED, job.getStatus());

        // then: step should be skipped
        for (ExecutedCmd cmd : stepService.list(job)) {
            Assert.assertEquals(ExecutedCmd.Status.SKIPPED, cmd.getStatus());
        }
    }

    private Job prepareJobForRunningStatus(Agent agent) {
        // init: job to mock the first node been send to agent
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        job.setAgentId(agent.getId());
        job.setCurrentPath(firstNode.getPath().getPathInStr());
        job.setStatus(Status.RUNNING);

        return jobDao.save(job);
    }
}