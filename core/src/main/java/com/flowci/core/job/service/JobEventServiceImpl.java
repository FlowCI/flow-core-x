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
import com.flowci.core.job.event.StopJobConsumerEvent;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.domain.Agent;
import com.flowci.exception.NotAvailableException;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import com.flowci.tree.StepNode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Log4j2
@Service
public class JobEventServiceImpl implements JobEventService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private RabbitQueueOperation callbackQueueManager;

    @Autowired
    private RabbitQueueOperation deadLetterQueueManager;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobActionService jobActionService;

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

            jobActionService.setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired while queued up");
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
                jobActionService.toStart(job);
            } catch (NotAvailableException e) {
                Job job = (Job) e.getExtra();
                jobActionService.setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });
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
        jobActionService.setJobStatusAndSave(job, Job.Status.CANCELLED, "Agent unexpected offline");
    }

    //====================================================================
    //        %% Rabbit events
    //====================================================================

    @Override
    public void handleCallback(ExecutedCmd step) {
        Job job = jobService.get(step.getJobId());
        jobActionService.toContinue(job, step);
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
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

    private void stopJobConsumer(Flow flow) {
        String queueName = flow.getQueueName();

        // remove queue manager and send Message.STOP_SIGN to consumer
        flowJobQueueManager.remove(queueName);

        // resume
        JobConsumerHandler handler = consumeHandlers.get(queueName);
        if (handler != null) {
            eventManager.publish(new StopJobConsumerEvent(this, flow.getId()));
        }

        consumeHandlers.remove(queueName);
    }

    /**
     * Job queue consumer for each flow
     */
    private class JobConsumerHandler implements Function<RabbitChannelOperation.Message, Boolean> {

        @Getter
        private final String queueName;

        JobConsumerHandler(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public Boolean apply(RabbitChannelOperation.Message message) {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                log.info("[Job Consumer] {} will be stopped", queueName);
                return true;
            }

            String jobId = new String(message.getBody());
            Job job = jobService.get(jobId);
            logInfo(job, "received from queue");

            jobActionService.toRun(job);
            return message.sendAck();
        }
    }
}
