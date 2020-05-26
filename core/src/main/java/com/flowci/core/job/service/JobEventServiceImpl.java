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
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.QueueOperations;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.StopJobConsumerEvent;
import com.flowci.domain.Agent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Log4j2
@Service
public class JobEventServiceImpl implements JobEventService {

    @Autowired
    private AppProperties.RabbitMQ rabbitProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private RabbitOperations jobsQueueManager;

    @Autowired
    private QueueOperations callbackQueueManager;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobActionService jobActionService;

    @Autowired
    private ThreadPoolTaskExecutor jobRunExecutor;

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener(FlowInitEvent.class)
    public void startJobQueueConsumers(FlowInitEvent event) {
        for (Flow flow : event.getFlows()) {
            declareJobQueueAndStartConsumer(flow);
        }
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startCallbackQueueConsumer(ContextRefreshedEvent ignore) throws IOException {
        callbackQueueManager.startConsumer(false, message -> {
            try {
                ExecutedCmd cmd = objectMapper.readValue(message.getBody(), ExecutedCmd.class);
                log.info("[Callback]: {}-{} = {}", cmd.getJobId(), cmd.getNodePath(), cmd.getStatus());

                handleCallback(cmd);
                return message.sendAck();
            } catch (IOException e) {
                log.error(e.getMessage());
                return false;
            }
        });
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startJobDeadLetterConsumer(ContextRefreshedEvent ignore) throws IOException {
        String deadLetterQueue = rabbitProperties.getJobDlQueue();
        jobsQueueManager.startConsumer(deadLetterQueue, true, message -> {
            String jobId = new String(message.getBody());
            Job job = jobService.get(jobId);
            jobActionService.toTimeout(job);
            return true;
        });
    }

    @EventListener
    public void onFlowCreated(FlowCreatedEvent event) {
        declareJobQueueAndStartConsumer(event.getFlow());
    }

    @EventListener
    public void onFlowDeleted(FlowDeletedEvent event) {
        stopJobConsumerAndDeleteQueue(event.getFlow());
        jobService.delete(event.getFlow());
    }

    @EventListener
    public void startNewJob(CreateNewJobEvent event) {
        jobRunExecutor.execute(() -> {
            Job job = jobService.create(event.getFlow(), event.getYml(), event.getTrigger(), event.getInput());
            jobActionService.toStart(job);
        });
    }

    @EventListener(value = AgentStatusEvent.class)
    public void updateJobAndStepWhenOffline(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (!agent.hasJob()) {
            return;
        }

        if (!agent.isOffline()) {
            return;
        }

        Job job = jobService.get(agent.getJobId());
        jobActionService.toCancelled(job, "Agent unexpected offline");
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

    private void declareJobQueueAndStartConsumer(Flow flow) {
        try {
            final String queue = flow.getQueueName();
            jobsQueueManager.declare(queue, true, 255, rabbitProperties.getJobDlExchange());

            jobsQueueManager.startConsumer(queue, false, message -> {
                String jobId = new String(message.getBody());
                Job job = jobService.get(jobId);
                logInfo(job, "received from queue");

                jobActionService.toRun(job);
                return message.sendAck();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopJobConsumerAndDeleteQueue(Flow flow) {
        String queue = flow.getQueueName();
        jobsQueueManager.removeConsumer(queue);
        eventManager.publish(new StopJobConsumerEvent(this, flow.getId()));
    }
}
