/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.domain.*;
import com.flowci.exception.NotAvailableException;
import com.flowci.tree.*;
import groovy.util.ScriptException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Log4j2
@Service
public class JobEventServiceImpl implements JobEventService {

    private static final Integer DefaultBeforeTimeout = 5;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private RabbitQueueOperation callbackQueueManager;

    @Autowired
    private RabbitQueueOperation deadLetterQueueManager;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobActionService jobStateService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private ThreadPoolTaskExecutor jobRunExecutor;

    private final Map<String, JobConsumerHandler> consumeHandlers = new ConcurrentHashMap<>();

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener(FlowInitEvent.class)
    public void startJobQueueConsumers(FlowInitEvent event) {
        for (Flow flow : event.getFlows()) {
            startJobConsumer(flow);
        }
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startCallbackQueueConsumer(ContextRefreshedEvent event) {
        RabbitChannelOperation.QueueConsumer consumer = callbackQueueManager.createConsumer((message -> {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                return true;
            }

            try {
                ExecutedCmd cmd = objectMapper.readValue(message.getBody(), ExecutedCmd.class);
                log.info("[Callback]: {}-{} = {}", cmd.getJobId(), cmd.getNodePath(), cmd.getStatus());

                handleCallback(cmd);
                return message.sendAck();
            } catch (IOException e) {
                log.error(e.getMessage());
                return false;
            }
        }));

        consumer.start(false);
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startJobTimeoutQueueConsumer(ContextRefreshedEvent e) {
        RabbitOperation.QueueConsumer consumer = deadLetterQueueManager.createConsumer(message -> {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                log.info("[Job Timeout Consumer] will be stopped");
                return true;
            }

            String jobId = new String(message.getBody());
            Job job = jobService.get(jobId);

            // not set to timeout if finished or cancelling
            if (Job.FINISH_STATUS.contains(job.getStatus()) || job.isCancelling()) {
                return true;
            }

            jobStateService.setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired while queued up");
            return true;
        });

        consumer.start(true);
    }

    @EventListener
    public void deleteJob(FlowDeletedEvent event) {
        stopJobConsumer(event.getFlow());
        jobService.delete(event.getFlow());
    }

    @EventListener
    public void startJobConsumer(FlowCreatedEvent event) {
        startJobConsumer(event.getFlow());
    }

    @EventListener
    public void startNewJob(CreateNewJobEvent event) {
        jobRunExecutor.execute(() -> {
            try {
                Job job = jobService.create(event.getFlow(), event.getYml(), event.getTrigger(), event.getInput());
                jobStateService.start(job);
            } catch (NotAvailableException e) {
                Job job = (Job) e.getExtra();
                jobStateService.setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });
    }

    @EventListener
    public void notifyToFindAvailableAgent(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (agent.getStatus() != Agent.Status.IDLE) {
            return;
        }

        if (!agent.hasJob()) {
            return;
        }

        // notify all consumer to find agent
        consumeHandlers.forEach((s, handler) -> handler.resume());
    }

    @EventListener(value = AgentStatusEvent.class)
    public void updateJobAndStep(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (agent.getStatus() != Agent.Status.OFFLINE || Objects.isNull(agent.getJobId())) {
            return;
        }

        Job job = jobService.get(agent.getJobId());
        if (job.isDone()) {
            return;
        }

        // update step status
        List<ExecutedCmd> steps = stepService.list(job);
        for (ExecutedCmd step : steps) {
            if (step.isRunning() || step.isPending()) {
                stepService.statusChange(step, ExecutedCmd.Status.SKIPPED, null);
            }
        }

        // update job status
        jobStateService.setJobStatusAndSave(job, Job.Status.CANCELLED, "Agent unexpected offline");
    }

    //====================================================================
    //        %% Rabbit events
    //====================================================================

    @Override
    public void handleCallback(ExecutedCmd execCmd) {
        // get cmd related job
        Job job = jobService.get(execCmd.getJobId());
        NodePath currentPath = NodePath.create(execCmd.getNodePath());

        // verify job node path is match cmd node path
        if (!currentPath.equals(currentNodePath(job))) {
            log.error("Invalid executed cmd callback: does not match job current node path");
            return;
        }

        if (!job.isRunning() && !job.isCancelling()) {
            log.error("Cannot handle cmd callback since job is not running: {}", job.getStatus());
            return;
        }

        NodeTree tree = ymlManager.getTree(job);
        StepNode node = tree.get(currentPath);

        // save executed cmd
        stepService.resultUpdate(execCmd);
        log.debug("Executed cmd {} been recorded", execCmd);

        updateJobTime(job, tree, node, execCmd);

        setJobContext(job, node, execCmd);

        // find next node
        StepNode next = findNext(job, tree, node, execCmd.isSuccess());
        Agent current = agentService.get(job.getAgentId());

        // job finished
        if (Objects.isNull(next)) {
            Job.Status statusFromContext = Job.Status.valueOf(job.getContext().get(Variables.Job.Status));
            jobStateService.setJobStatusAndSave(job, statusFromContext, execCmd.getError());

            agentService.tryRelease(current);
            logInfo(job, "finished with status {}", statusFromContext);
            return;
        }

        // continue to run next node
        job.setCurrentPath(next.getPathAsString());

        log.debug("Send job {} step {} to agent", job.getKey(), node.getName());
        saveJobAndSendToAgent(job, next, current);
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }

    private void updateJobTime(Job job, NodeTree tree, Node node, ExecutedCmd cmd) {
        if (tree.isFirst(node.getPath())) {
            job.setStartAt(cmd.getStartAt());
        }

        job.setFinishAt(cmd.getFinishAt());
    }

    private void setJobContext(Job job, StepNode node, ExecutedCmd cmd) {
        // merge output to job context
        Vars<String> context = job.getContext();
        context.merge(cmd.getOutput());

        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());
        context.put(Variables.Job.Steps, stepService.toVarString(job, node));

        // after status not apart of job status
        if (!node.isAfter()) {
            context.put(Variables.Job.Status, StatusHelper.convert(cmd).name());
        }
    }

    private StepNode findNext(Job job, NodeTree tree, Node current, boolean isSuccess) {
        StepNode next = tree.next(current.getPath());
        if (Objects.isNull(next)) {
            return null;
        }

        // find step from after
        if (!isSuccess && !next.isAfter()) {
            return findNext(job, tree, next, false);
        }

        // Execute before condition to check the next node should be skipped or not
        if (executeBeforeCondition(job, next)) {
            return next;
        }

        return findNext(job, tree, next, isSuccess);
    }

    private void startJobConsumer(Flow flow) {
        String queueName = flow.getQueueName();

        JobConsumerHandler handler = new JobConsumerHandler(queueName);
        consumeHandlers.put(queueName, handler);

        RabbitQueueOperation manager = flowJobQueueManager.create(queueName);
        RabbitOperation.QueueConsumer consumer = manager.createConsumer(queueName, handler);

        // start consumer
        consumer.start(false);
    }

    private void dispatch(Job job, Agent available) {
        // reload the job to get latest status before dispatch
        job = jobService.get(job.getId());

        if (!job.isQueuing()) {
            logInfo(job, "don't dispatch since status is not queuing");
            return;
        }

        NodeTree tree = ymlManager.getTree(job);
        StepNode next = tree.next(currentNodePath(job));

        // do not accept job without regular steps
        if (Objects.isNull(next)) {
            log.debug("Next node cannot be found when process job {}", job);
            return;
        }

        log.debug("Next step of job {} is {}", job.getId(), next.getName());

        // set path, agent id, agent name and status to job
        job.setCurrentPath(next.getPathAsString());
        job.setAgentId(available.getId());
        job.setAgentSnapshot(available);
        jobStateService.setJobStatusAndSave(job, Job.Status.RUNNING, null);

        // execute condition script
        Boolean executed = executeBeforeCondition(job, next);
        if (!executed) {
            ExecutedCmd executedCmd = stepService.get(job, next);
            handleCallback(executedCmd);
            return;
        }

        // dispatch job to agent queue
        saveJobAndSendToAgent(job, next, available);
    }

    private Boolean executeBeforeCondition(Job job, StepNode node) {
        if (!node.hasBefore()) {
            return true;
        }

        Vars<String> map = new StringVars()
                .merge(job.getContext())
                .merge(node.getEnvironments());

        try {
            GroovyRunner<Boolean> runner = GroovyRunner.create(DefaultBeforeTimeout, node.getBefore(), map);
            Boolean result = runner.run();

            if (Objects.isNull(result) || result == Boolean.FALSE) {
                ExecutedCmd.Status newStatus = ExecutedCmd.Status.SKIPPED;
                String errMsg = "The 'before' condition cannot be matched";
                stepService.statusChange(job, node, newStatus, errMsg);
                return false;
            }

            return true;
        } catch (ScriptException e) {
            stepService.statusChange(job, node, ExecutedCmd.Status.SKIPPED, e.getMessage());
            return false;
        }
    }

    private Agent findAvailableAgent(Job job) {
        Set<String> agentTags = job.getAgentSelector().getTags();
        List<Agent> agents = agentService.find(Agent.Status.IDLE, agentTags);

        if (agents.isEmpty()) {
            return null;
        }

        Iterator<Agent> availableList = agents.iterator();

        // try to lock it
        while (availableList.hasNext()) {
            Agent agent = availableList.next();
            agent.setJobId(job.getId());

            if (agentService.tryLock(agent)) {
                return agent;
            }

            availableList.remove();
        }

        return null;
    }

    /**
     * Send step to agent
     */
    private void saveJobAndSendToAgent(Job job, StepNode node, Agent agent) {
        // set executed cmd step to running
        ExecutedCmd executedCmd = stepService.get(job, node);

        try {
            if (!executedCmd.isRunning()) {
                stepService.statusChange(job, node, ExecutedCmd.Status.RUNNING, null);
            }

            jobStateService.setJobStatusAndSave(job, job.getStatus(), null);

            CmdIn cmd = cmdManager.createShellCmd(job, node, executedCmd);
            agentService.dispatch(cmd, agent);
            logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
        } catch (Throwable e) {
            log.debug("Fail to dispatch job {} to agent {}", job.getId(), agent.getId(), e);

            // set current step to exception
            stepService.statusChange(job, node, ExecutedCmd.Status.EXCEPTION, null);

            // set current job failure
            jobStateService.setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            agentService.tryRelease(agent);
        }
    }

    private NodePath currentNodePath(Job job) {
        return NodePath.create(job.getCurrentPath());
    }

    private void stopJobConsumer(Flow flow) {
        String queueName = flow.getQueueName();

        // remove queue manager and send Message.STOP_SIGN to consumer
        flowJobQueueManager.remove(queueName);

        // resume
        JobConsumerHandler handler = consumeHandlers.get(queueName);
        if (handler != null) {
            handler.resume();
        }

        consumeHandlers.remove(queueName);
    }

    /**
     * Job queue consumer for each flow
     */
    private class JobConsumerHandler implements Function<RabbitChannelOperation.Message, Boolean> {

        private final static long RetryIntervalOnNotFound = 30 * 1000; // 60 seconds

        private final Object lock = new Object();

        @Getter
        private final String queueName;

        // Message.STOP_SIGN will be coming from other thread
        private final AtomicBoolean isStop = new AtomicBoolean(false);

        JobConsumerHandler(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public Boolean apply(RabbitChannelOperation.Message message) {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                log.info("[Job Consumer] {} will be stopped", queueName);
                isStop.set(true);
                resume();
                return true;
            }

            String jobId = new String(message.getBody());
            Job job = jobService.get(jobId);
            logInfo(job, "received from queue");
            eventManager.publish(new JobReceivedEvent(this, job));

            if (!job.isQueuing()) {
                logInfo(job, "can't handle since status is not in queuing");
                return false;
            }

            Agent available;

            while ((available = findAvailableAgent(job)) == null) {
                logInfo(job, "waiting for agent...");
                eventManager.publish(new NoIdleAgentEvent(this, job));

                synchronized (lock) {
                    ThreadHelper.wait(lock, RetryIntervalOnNotFound);
                }

                if (isStop.get()) {
                    return false;
                }

                if (job.isExpired()) {
                    jobStateService.setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired while waiting for agent");
                    logInfo(job, "expired");
                    return false;
                }
            }

            dispatch(job, available);
            return message.sendAck();
        }

        void resume() {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }
}
