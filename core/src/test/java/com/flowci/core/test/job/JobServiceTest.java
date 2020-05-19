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

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Notification;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.service.JobActionService;
import com.flowci.core.job.service.JobEventService;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.task.event.StartAsyncLocalTaskEvent;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.*;
import com.flowci.tree.*;
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
    private AgentDao agentDao;

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
    private JobActionService jobActionService;

    @Autowired
    private YmlManager ymlManager;

    private Flow flow;

    private Yml yml;

    @Before
    public void mockFlowAndYml() throws IOException {
        mockLogin();

        flow = flowService.create("hello");
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow.yml")));
    }

    @Test
    public void should_create_job_with_expected_context() {
        // init:
        flow.getLocally().put("LOCAL_VAR", VarValue.of("local", VarType.STRING));
        flow.getNotifications().add(new Notification().setPlugin("email"));
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

        // then: notification should be added
        Assert.assertEquals(1, job.getNotifications().size());
        Assert.assertEquals("email", job.getNotifications().get(0).getPlugin());
    }

    @Test
    public void should_init_steps_cmd_after_job_created() {
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        List<ExecutedCmd> steps = stepService.list(job);
        Assert.assertNotNull(steps);
        Assert.assertEquals(2, steps.size());

        for (ExecutedCmd step : steps) {
            Assert.assertNotNull(step.getFlowId());
            Assert.assertNotNull(step.getJobId());
            Assert.assertNotNull(step.getNodePath());
            Assert.assertNotNull(step.getBuildNumber());
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

        jobActionService.toStart(job);
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
        Assert.assertFalse(job.isExpired());
    }

    @Test
    public void should_finish_whole_job() throws InterruptedException {
        // init:
        Agent agent = agentService.create("hello.agent", null, Optional.empty());
        mockAgentOnline(agentService.getPath(agent));

        flow.getNotifications().add(new Notification().setPlugin("email"));
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        CountDownLatch localTaskCountDown = new CountDownLatch(1);
        addEventListener((ApplicationListener<StartAsyncLocalTaskEvent>) event -> {
            localTaskCountDown.countDown();
        });

        FlowNode root = YmlParser.load(flow.getName(), yml.getRaw());
        NodeTree tree = NodeTree.create(root);

        StepNode firstNode = tree.next(tree.getRoot().getPath());
        ExecutedCmd firstStep = stepService.get(job.getId(), firstNode.getPathAsString());

        StepNode secondNode = tree.next(firstNode.getPath());
        ExecutedCmd secondStep = stepService.get(job.getId(), secondNode.getPathAsString());

        // when:
        ObjectWrapper<Agent> agentForStep1 = new ObjectWrapper<>();
        ObjectWrapper<CmdIn> cmdForStep1 = new ObjectWrapper<>();
        CountDownLatch counterForStep1 = new CountDownLatch(1);

        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            if (event.getCmd().getId().equals(firstStep.getId())) {
                agentForStep1.setValue(event.getAgent());
                cmdForStep1.setValue(event.getCmd());
                counterForStep1.countDown();
            }
        });

        ObjectWrapper<Agent> agentForStep2 = new ObjectWrapper<>();
        ObjectWrapper<CmdIn> cmdForStep2 = new ObjectWrapper<>();
        CountDownLatch counterForStep2 = new CountDownLatch(1);

        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            if (event.getCmd().getId().equals(secondStep.getId())) {
                agentForStep2.setValue(event.getAgent());
                cmdForStep2.setValue(event.getCmd());
                counterForStep2.countDown();
            }
        });

        jobActionService.toStart(job);
        Assert.assertTrue(counterForStep1.await(10, TimeUnit.SECONDS));

        // then: verify step 1 agent
        Assert.assertEquals(agent, agentForStep1.getValue());
        Assert.assertEquals(job.getId(), agentForStep1.getValue().getJobId());
        Assert.assertEquals(Agent.Status.BUSY, agentForStep1.getValue().getStatus());

        // then: verify job status should be running
        job = jobService.get(job.getId());
        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(firstStep.getNodePath(), job.getCurrentPath());

        // then: verify step 1 cmd has been sent
        CmdIn cmd = cmdForStep1.getValue();
        Assert.assertEquals(firstStep.getId(), cmd.getId());
        Assert.assertTrue(cmd.isAllowFailure());
        Assert.assertEquals("echo step version", cmd.getInputs().get("FLOW_VERSION"));
        Assert.assertEquals("echo step", cmd.getInputs().get("FLOW_WORKSPACE"));
        Assert.assertEquals("echo hello\n", cmd.getScripts().get(0));

        // when: make dummy response from agent for step 1
        firstStep.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(firstStep);

        // then: verify step 2 agent
        Assert.assertTrue(counterForStep2.await(10, TimeUnit.SECONDS));

        Assert.assertEquals(agent, agentForStep2.getValue());
        Assert.assertEquals(job.getId(), agentForStep2.getValue().getJobId());
        Assert.assertEquals(Agent.Status.BUSY, agentForStep2.getValue().getStatus());

        // then: verify job status should be running
        job = jobService.get(job.getId());
        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(secondStep.getNodePath(), job.getCurrentPath());

        // then: verify step 1 cmd has been sent
        cmd = cmdForStep2.getValue();
        Assert.assertEquals(secondStep.getId(), cmd.getId());
        Assert.assertFalse(cmd.isAllowFailure());
        Assert.assertEquals("echo 2", cmd.getScripts().get(0));

        // when: make dummy response from agent for step 2
        secondStep.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmdDao.save(secondStep);
        jobEventService.handleCallback(secondStep);

        // // then: should job with SUCCESS status and sent notification task
        Assert.assertEquals(Status.SUCCESS, jobService.get(job.getId()).getStatus());
        Assert.assertTrue(localTaskCountDown.await(60, TimeUnit.SECONDS));
    }

    @Test
    public void should_handle_cmd_callback_for_success_status() {
        // init: agent and job
        Agent agent = agentService.create("hello.agent", null, Optional.empty());
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        StepNode firstNode = tree.next(tree.getRoot().getPath());
        ExecutedCmd firstStep = stepService.get(job.getId(), firstNode.getPathAsString());

        // when: cmd of first node been executed
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        firstStep.setStatus(ExecutedCmd.Status.SUCCESS);
        firstStep.setOutput(output);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(firstStep);

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // then: job current context should be updated
        StepNode secondNode = tree.next(firstNode.getPath());
        Assert.assertEquals(secondNode.getPath(), NodePath.create(job.getCurrentPath()));
        ExecutedCmd secondStep = stepService.get(job.getId(), secondNode.getPathAsString());

        // when: cmd of second node been executed
        output = new StringVars();
        output.put("HELLO_JAVA", "hello.java");
        secondStep.setStatus(ExecutedCmd.Status.SUCCESS);
        secondStep.setOutput(output);
        executedCmdDao.save(secondStep);
        jobEventService.handleCallback(secondStep);

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
        StepNode firstNode = tree.next(tree.getRoot().getPath());
        ExecutedCmd firstStep = stepService.get(job.getId(), firstNode.getPathAsString());

        // when: cmd of first node with failure
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        firstStep.setStatus(ExecutedCmd.Status.EXCEPTION);
        firstStep.setOutput(output);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(firstStep);

        // then: job status should be running and current path should be change to second node
        job = jobDao.findById(job.getId()).get();
        StepNode secondNode = tree.next(firstNode.getPath());
        ExecutedCmd secondCmd = stepService.get(job.getId(), secondNode.getPathAsString());

        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(secondNode.getPathAsString(), job.getCurrentPath());
        Assert.assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // when: second cmd of node been timeout
        output = new StringVars();
        output.put("HELLO_TIMEOUT", "hello.timeout");

        secondCmd.setStatus(ExecutedCmd.Status.TIMEOUT);
        secondCmd.setOutput(output);
        executedCmdDao.save(secondCmd);
        jobEventService.handleCallback(secondCmd);

        // then: job should be timeout with error message
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.SUCCESS, job.getStatus());
        Assert.assertEquals("hello.timeout", job.getContext().get("HELLO_TIMEOUT"));
    }

    @Test
    public void should_job_failure_with_after() throws Exception {
        yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-failure-with-after.yml")));
        Agent agent = agentService.create("hello.agent.0", null, Optional.empty());
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        StepNode firstNode = tree.next(tree.getRoot().getPath());

        // when: set first step as failure status
        ExecutedCmd firstStep = stepService.get(job.getId(), firstNode.getPathAsString());
        firstStep.setStatus(ExecutedCmd.Status.EXCEPTION);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(firstStep);

        // when: set final node as success status
        StepNode secondNode = tree.next(firstNode.getPath());
        ExecutedCmd secondStep = stepService.get(job.getId(), secondNode.getPathAsString());
        secondStep.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmdDao.save(secondStep);
        jobEventService.handleCallback(secondStep);

        // then: job status should be failure since final node does not count to step
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.FAILURE, job.getStatus());
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
        jobActionService.toStart(job);

        CountDownLatch waitForRunning = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobStatusChangeEvent>) event -> {
            if (event.getJob().getStatus() == Status.RUNNING) {
                waitForRunning.countDown();
            }
        });

        waitForRunning.await(10, TimeUnit.SECONDS);
        job = jobService.get(job.getId());
        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(agent.getId(), job.getAgentId());

        // when: agent status change to offline
        CountDownLatch waitForCancelled = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobStatusChangeEvent>) event -> {
            if (event.getJob().getStatus() == Status.CANCELLED) {
                waitForCancelled.countDown();
            }
        });

        agent.setJobId(job.getId());
        agent.setStatus(Agent.Status.OFFLINE);
        agentDao.save(agent); // persistent agent status to db

        multicastEvent(new AgentStatusEvent(this, agent));

        // then: job should be cancelled
        waitForCancelled.await();
        job = jobService.get(job.getId());
        Assert.assertEquals(Status.CANCELLED, job.getStatus());

        // then: step should be skipped
        for (ExecutedCmd cmd : stepService.list(job)) {
            Assert.assertEquals(ExecutedCmd.Status.SKIPPED, cmd.getStatus());
        }
    }

    @Test
    public void should_rerun_job() {
        // init: create old job wit success status
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);
        job.getContext().put(com.flowci.core.trigger.domain.Variables.GIT_COMMIT_ID, "111222333");
        job.setStatus(Status.SUCCESS);
        job.setStatusToContext(Status.SUCCESS);
        jobDao.save(job);

        // when: rerun
        job = jobService.rerun(flow, job);
        Assert.assertNotNull(job);

        // then: verify context
        Vars<String> context = job.getContext();
        Assert.assertEquals(Status.QUEUED.toString(), context.get(Variables.Job.Status));
        Assert.assertEquals("1", context.get(Variables.Job.BuildNumber));
        Assert.assertEquals("111222333", context.get(com.flowci.core.trigger.domain.Variables.GIT_COMMIT_ID));
        Assert.assertNotNull(context.get(Variables.Job.Trigger));
        Assert.assertNotNull(context.get(Variables.Job.TriggerBy));

        // then: verify job properties
        Assert.assertEquals(flow.getName(), job.getCurrentPath());
        Assert.assertFalse(job.isExpired());
        Assert.assertNotNull(job.getCreatedAt());
        Assert.assertNotNull(job.getCreatedBy());
        Assert.assertEquals(Trigger.MANUAL, job.getTrigger());

        Assert.assertNull(job.getFinishAt());
        Assert.assertNull(job.getStartAt());
        Assert.assertNull(job.getAgentId());
        Assert.assertNull(job.getAgentInfo());
    }

    private Job prepareJobForRunningStatus(Agent agent) {
        // init: job to mock the first node been send to agent
        Job job = jobService.create(flow, yml.getRaw(), Trigger.MANUAL, StringVars.EMPTY);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        job.setAgentId(agent.getId());
        job.setCurrentPath(firstNode.getPathAsString());
        job.setStatus(Status.RUNNING);
        job.setStatusToContext(Status.RUNNING);

        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(Status.RUNNING, job.getStatusFromContext());

        return jobDao.save(job);
    }
}