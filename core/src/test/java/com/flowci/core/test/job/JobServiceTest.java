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

import com.flowci.common.domain.*;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.domain.*;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobAgentDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobDesc;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.service.JobActionService;
import com.flowci.core.job.service.JobEventService;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.core.plugin.dao.PluginDao;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.tree.FlowNode;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
@Slf4j
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JobServiceTest extends ZookeeperScenario {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private JobAgentDao jobAgentDao;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private PluginDao pluginDao;

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

    private FlowYml ymlEntity;

    @BeforeEach
    void mockFlowAndYml() throws IOException {
        var yaml = StringHelper.toString(load("flow.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(yaml));
        flow = flowService.create("hello", option);
        ymlEntity = ymlService.get(flow.getId());
    }

    @Test
    void should_create_job_with_expected_context() {
        // init:
        flow.getVars().put("LOCAL_VAR", VarValue.of("local", VarType.STRING));
        flowDao.save(flow);
        flow = flowService.get(flow.getName());

        StringVars input = new StringVars();
        input.put("INPUT_VAR", "input");
        input.put(Variables.Git.EVENT_ID, "dummy_git_event_id");

        // when: create job
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, input);
        Vars<String> context = job.getContext();
        assertNotNull(context);

        // then: related job should be added
        List<JobDesc> relatedList = jobService.listRelated(job);
        assertEquals(1, relatedList.size());
        assertEquals(job.getId(), relatedList.get(0).getId());

        // then: default vars
        assertTrue(context.containsKey(Variables.Flow.Name));
        assertTrue(context.containsKey(Variables.App.ServerUrl));

        assertTrue(context.containsKey(Variables.Job.Status));
        assertTrue(context.containsKey(Variables.Job.BuildNumber));
        assertTrue(context.containsKey(Variables.Job.Trigger));
        assertTrue(context.containsKey(Variables.Job.TriggerBy));
        assertTrue(context.containsKey(Variables.Job.StartAt));
        assertTrue(context.containsKey(Variables.Job.FinishAt));
        assertTrue(context.containsKey(Variables.Job.DurationInSeconds));

        // then: vars should be included in job context
        assertTrue(context.containsKey("LOCAL_VAR"));
        assertTrue(context.containsKey("INPUT_VAR"));
        assertTrue(context.containsKey("FLOW_WORKSPACE"));
        assertTrue(context.containsKey("FLOW_VERSION"));
    }

    @Test
    void should_init_steps_cmd_after_job_created() {
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);

        List<Step> steps = stepService.list(job);
        assertNotNull(steps);
        assertEquals(3, steps.size());

        for (Step step : steps) {
            assertNotNull(step.getFlowId());
            assertNotNull(step.getJobId());
            assertNotNull(step.getNodePath());
            assertNotNull(step.getBuildNumber());
        }
    }

    @Test
    void should_start_new_job() throws Throwable {
        ObjectWrapper<Job> receivedJob = new ObjectWrapper<>();

        // init: register JobReceivedEvent
        CountDownLatch waitForJobFromQueue = new CountDownLatch(1);
        addEventListener((ApplicationListener<JobReceivedEvent>) event -> {
            receivedJob.setValue(event.getJob());
            waitForJobFromQueue.countDown();
        });

        // when: create and start job
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);
        job = jobService.get(job.getId());

        assertEquals(Status.CREATED, job.getStatus());
        assertTrue(job.getCurrentPath().isEmpty());

        jobActionService.toStart(job.getId());
        job = jobService.get(job.getId());
        assertEquals(Status.QUEUED, job.getStatus());

        assertNotNull(job);
        assertNotNull(ymlManager.getTree(job));

        // then: confirm job is received from queue
        waitForJobFromQueue.await(10, TimeUnit.SECONDS);
        assertEquals(0, waitForJobFromQueue.getCount());
        assertEquals(job, receivedJob.getValue());
    }

    @Test
    void should_get_job_expire() {
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);
        assertFalse(job.isExpired());
    }

    @Test
    void should_finish_whole_job() throws InterruptedException, IOException {
        // init:
        Plugin p = new Plugin();
        p.setName("email"); // from yaml
        p.setVersion(Version.parse("0.1.1"));
        pluginDao.save(p);

        String yaml = StringHelper.toString(load("flow.yml"));
        ymlEntity = ymlService.saveYml(flow, List.of(new SimpleYml("default", StringHelper.toBase64(yaml))));

        Agent agent = agentService.create(new AgentOption().setName("hello.agent"));
        mockAgentOnline(agent.getToken());

        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);

        FlowNode root = YmlParser.load(FlowYml.toRawArray(ymlEntity.getList()));
        NodeTree tree = NodeTree.create(root);

        Node firstNode = tree.getRoot().getNext().get(0);
        Step firstStep = stepService.get(job.getId(), firstNode.getPath().getPathInStr());

        Node secondNode = firstNode.getNext().get(0);
        Step secondStep = stepService.get(job.getId(), secondNode.getPath().getPathInStr());

        // when:
        ObjectWrapper<Agent> agentForStep1 = new ObjectWrapper<>();
        ObjectWrapper<CmdIn> cmdForStep1 = new ObjectWrapper<>();
        CountDownLatch counterForStep1 = new CountDownLatch(1);

        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            CmdIn in = event.getCmd();
            if (in instanceof ShellIn) {
                ShellIn shellIn = (ShellIn) in;

                if (shellIn.getId().equals(firstStep.getId())) {
                    agentForStep1.setValue(event.getAgent());
                    cmdForStep1.setValue(event.getCmd());
                    counterForStep1.countDown();
                }
            }
        });

        ObjectWrapper<Agent> agentForStep2 = new ObjectWrapper<>();
        ObjectWrapper<CmdIn> cmdForStep2 = new ObjectWrapper<>();
        CountDownLatch counterForStep2 = new CountDownLatch(1);

        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            CmdIn in = event.getCmd();
            if (in instanceof ShellIn) {
                ShellIn shellIn = (ShellIn) in;
                if (shellIn.getId().equals(secondStep.getId())) {
                    agentForStep2.setValue(event.getAgent());
                    cmdForStep2.setValue(event.getCmd());
                    counterForStep2.countDown();
                }
            }
        });

        jobActionService.toStart(job.getId());
        assertTrue(counterForStep1.await(10, TimeUnit.SECONDS));

        // then: verify step 1 agent
        assertEquals(agent, agentForStep1.getValue());
        assertEquals(job.getId(), agentForStep1.getValue().getJobId());
        assertEquals(Agent.Status.BUSY, agentForStep1.getValue().getStatus());

        // then: verify job status should be running
        job = jobService.get(job.getId());
        assertEquals(Status.RUNNING, job.getStatus());
        assertTrue(job.getCurrentPath().contains(firstStep.getNodePath()));

        // then: verify step 1 cmd has been sent
        ShellIn cmd = (ShellIn) cmdForStep1.getValue();
        assertEquals(firstStep.getId(), cmd.getId());
        assertTrue(cmd.isAllowFailure());
        assertEquals("echo step version", cmd.getInputs().get("FLOW_VERSION"));
        assertEquals("echo step", cmd.getInputs().get("FLOW_WORKSPACE"));
        assertEquals("echo hello\n", cmd.getBash().get(0));

        // when: make dummy response from agent for step 1
        firstStep.setStatus(Step.Status.SUCCESS);
        firstStep.setFinishAt(new Date());

        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(getShellOutFromStep(firstStep));

        // then: verify step 2 agent
        assertTrue(counterForStep2.await(10, TimeUnit.SECONDS));

        assertEquals(agent, agentForStep2.getValue());
        assertEquals(job.getId(), agentForStep2.getValue().getJobId());
        assertEquals(Agent.Status.BUSY, agentForStep2.getValue().getStatus());

        // then: verify job status should be running
        job = jobService.get(job.getId());
        assertEquals(Status.RUNNING, job.getStatus());
        assertTrue(job.getCurrentPath().contains(secondStep.getNodePath()));

        // then: verify step 1 cmd has been sent
        cmd = (ShellIn) cmdForStep2.getValue();
        assertEquals(secondStep.getId(), cmd.getId());
        assertFalse(cmd.isAllowFailure());
        assertEquals("echo 2", cmd.getBash().get(0));

        // when: make dummy response from agent for step 2
        secondStep.setStatus(Step.Status.SUCCESS);
        secondStep.setFinishAt(new Date());

        executedCmdDao.save(secondStep);
        jobEventService.handleCallback(getShellOutFromStep(secondStep));

        // // then: should job with SUCCESS status and sent notification task
        assertEquals(Status.SUCCESS, jobService.get(job.getId()).getStatus());
    }

    @Test
    void should_handle_cmd_callback_for_success_status() {
        // init: agent and job
        Agent agent = agentService.create(new AgentOption().setName("hello.agent"));
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.getRoot().getNext().get(0);
        Step firstStep = stepService.get(job.getId(), firstNode.getPath().getPathInStr());

        // when: cmd of first node been executed
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        firstStep.setStatus(Step.Status.SUCCESS);
        firstStep.setOutput(output);

        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(getShellOutFromStep(firstStep));

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        assertEquals(Status.RUNNING, job.getStatus());
        assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // then: job current context should be updated
        Node secondNode = firstNode.getNext().get(0);
        assertTrue(job.getCurrentPath().contains(secondNode.getPathAsString()));
        Step secondStep = stepService.get(job.getId(), secondNode.getPath().getPathInStr());

        // when: cmd of second node been executed
        output = new StringVars();
        output.put("HELLO_JAVA", "hello.java");
        secondStep.setStatus(Step.Status.SUCCESS);
        secondStep.setOutput(output);
        executedCmdDao.save(secondStep);
        jobEventService.handleCallback(getShellOutFromStep(secondStep));

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        assertEquals("hello.java", job.getContext().get("HELLO_JAVA"));
        assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));
        assertEquals(Status.SUCCESS, job.getStatus());
    }

    @Test
    void should_handle_cmd_callback_for_failure_status() throws IOException {
        // init: agent and job
        String yaml = StringHelper.toString(load("flow-with-failure.yml"));
        ymlEntity = ymlService.saveYml(flow, List.of(new SimpleYml("default", StringHelper.toBase64(yaml))));

        Agent agent = agentService.create(new AgentOption().setName("hello.agent"));
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.getRoot().getNext().get(0);
        Step firstStep = stepService.get(job.getId(), firstNode.getPath().getPathInStr());

        // when: cmd of first node with failure
        firstStep.setStatus(Step.Status.EXCEPTION);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(getShellOutFromStep(firstStep));

        // then: job should be failure
        job = jobDao.findById(job.getId()).get();
        assertEquals(Status.FAILURE, job.getStatus());
        assertTrue(job.getCurrentPath().contains("flow/step-1"));
    }

    @Test
    void should_handle_cmd_callback_for_failure_status_but_allow_failure() throws IOException {
        // init: agent and job
        String yaml = StringHelper.toString(load("flow-all-failure.yml"));
        ymlEntity = ymlService.saveYml(flow, List.of(new SimpleYml("default", StringHelper.toBase64(yaml))));

        Agent agent = agentService.create(new AgentOption().setName("hello.agent"));
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.getRoot().getNext().get(0);
        Step firstStep = stepService.get(job.getId(), firstNode.getPath().getPathInStr());

        // when: cmd of first node with failure
        StringVars output = new StringVars();
        output.put("HELLO_WORLD", "hello.world");

        firstStep.setStatus(Step.Status.EXCEPTION);
        firstStep.setOutput(output);
        executedCmdDao.save(firstStep);
        jobEventService.handleCallback(getShellOutFromStep(firstStep));

        // then: job status should be running and current path should be change to second node
        job = jobDao.findById(job.getId()).get();
        Node secondNode = firstNode.getNext().get(0);
        Step secondCmd = stepService.get(job.getId(), secondNode.getPath().getPathInStr());

        assertEquals(Status.RUNNING, job.getStatus());
        assertTrue(job.getCurrentPath().contains(secondNode.getPathAsString()));
        assertEquals("hello.world", job.getContext().get("HELLO_WORLD"));

        // when: second cmd of node been timeout
        output = new StringVars();
        output.put("HELLO_TIMEOUT", "hello.timeout");

        secondCmd.setStatus(Step.Status.TIMEOUT);
        secondCmd.setOutput(output);
        executedCmdDao.save(secondCmd);
        jobEventService.handleCallback(getShellOutFromStep(secondCmd));

        // then: job should be timeout with error message
        job = jobDao.findById(job.getId()).get();
        assertEquals(Status.SUCCESS, job.getStatus());
        assertEquals("hello.timeout", job.getContext().get("HELLO_TIMEOUT"));
    }

    @Test
    @Timeout(value = 30)
    void should_cancel_job_if_agent_offline() throws IOException, InterruptedException {
        // init:
        String yaml = StringHelper.toString(load("flow-with-condition.yml"));
        ymlEntity = ymlService.saveYml(flow, List.of(new SimpleYml("default", StringHelper.toBase64(yaml))));

        // mock agent online
        Agent agent = agentService.create(new AgentOption().setName("hello.agent.2"));
        mockAgentOnline(agent.getToken());

        // given: start job and wait for running
        Job job = prepareJobForRunningStatus(agent);

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
        assertEquals(Status.CANCELLED, job.getStatus());

        // then: step should be skipped
        List<Step> steps = stepService.list(job);
        for (Step step : steps) {
            assertEquals(Step.Status.PENDING, step.getStatus());
        }
    }

    @Test
    void should_rerun_job() {
        // init: create old job wit success status
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);
        job.setStatus(Status.SUCCESS);
        JobContextHelper.setStatus(job, Status.SUCCESS);
        jobDao.save(job);

        // when: rerun
        job = jobService.rerun(flow, job);
        assertNotNull(job);

        // then: verify context
        Vars<String> context = job.getContext();
        assertEquals(Status.QUEUED.toString(), context.get(Variables.Job.Status));
        assertEquals("1", context.get(Variables.Job.BuildNumber));
        assertNotNull(context.get(Variables.Job.Trigger));
        assertNotNull(context.get(Variables.Job.TriggerBy));

        // then: verify job properties
        assertTrue(job.getCurrentPath().isEmpty());
        assertFalse(job.isExpired());
        assertNotNull(job.getCreatedAt());
        assertNotNull(job.getCreatedBy());
        assertEquals(Trigger.MANUAL, job.getTrigger());

        assertNull(job.getFinishAt());
        assertNull(job.getStartAt());
        assertTrue(job.getSnapshots().isEmpty());
    }

    private Job prepareJobForRunningStatus(Agent agent) {
        // init: job to mock the first node been sent to agent
        Job job = jobService.create(flow, ymlEntity.getList(), Trigger.MANUAL, StringVars.EMPTY);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.getRoot().getNext().get(0);

        jobAgentDao.addFlowToAgent(job.getId(), agent.getId(), tree.getRoot().getPathAsString());

        job.resetCurrentPath().getCurrentPath().add(firstNode.getPathAsString());
        job.setStatus(Status.RUNNING);
        JobContextHelper.setStatus(job, Status.RUNNING);

        assertEquals(Status.RUNNING, job.getStatus());
        assertEquals(Status.RUNNING, JobContextHelper.getStatus(job));

        return jobDao.save(job);
    }

    private ShellOut getShellOutFromStep(Step step) {
        return new ShellOut()
                .setId(step.getId())
                .setStatus(step.getStatus())
                .setOutput(step.getOutput())
                .setFinishAt(new Date());
    }
}