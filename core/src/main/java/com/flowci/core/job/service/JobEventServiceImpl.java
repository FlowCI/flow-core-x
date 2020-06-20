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
import com.flowci.core.agent.domain.CmdOut;
import com.flowci.core.agent.domain.ShellOut;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.QueueOperations;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.StopJobConsumerEvent;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.domain.Agent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

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
    private StepService stepService;

    @Autowired
    private JobActionService jobActionService;

    @Autowired
    private ThreadPoolTaskExecutor jobStartExecutor;

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
        jobStartExecutor.execute(() -> {
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

    @Override
    public void handleCallback(Step step) {
        Job job = jobService.get(step.getJobId());
        jobActionService.toContinue(job, step);
    }

    //====================================================================
    //        %% Rabbit events
    //====================================================================

    @EventListener(value = ContextRefreshedEvent.class)
    public void startCallbackQueueConsumer() throws IOException {
        callbackQueueManager.startConsumer(false, message -> {
            byte[] raw = message.getBody();
            byte ind = raw[0];
            byte[] body = Arrays.copyOfRange(raw, 1, raw.length);

            try {
                switch (ind) {
                    case CmdOut.ShellOutInd:
                        ShellOut shellOut = objectMapper.readValue(body, ShellOut.class);
                        Step step = stepService.get(shellOut.getId());
                        step.setFrom(shellOut);

                        log.info("[Callback]: {}-{} = {}", step.getJobId(), step.getNodePath(), step.getStatus());
                        handleCallback(step);
                        break;

                    case CmdOut.TtyOutInd:
                        TtyCmd.Out ttyOut = objectMapper.readValue(body, TtyCmd.Out.class);
                        eventManager.publish(new TtyStatusUpdateEvent(this, ttyOut));
                        break;

                    default:
                        log.warn("Invalid message from callback queue: {}", new String(raw));
                }
            } catch (IOException e) {
                log.warn("Unable to decode message from callback queue: {}", new String(raw));
            }

            return message.sendAck();
        });
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startJobDeadLetterConsumer() throws IOException {
        String deadLetterQueue = rabbitProperties.getJobDlQueue();
        jobsQueueManager.startConsumer(deadLetterQueue, true, message -> {
            String jobId = new String(message.getBody());
            Job job = jobService.get(jobId);
            jobActionService.toTimeout(job);
            return true;
        });
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
            }, jobRunExecutor);
        } catch (IOException e) {
            log.warn(e);
        }
    }

    private void stopJobConsumerAndDeleteQueue(Flow flow) {
        String queue = flow.getQueueName();
        jobsQueueManager.removeConsumer(queue);
        eventManager.publish(new StopJobConsumerEvent(this, flow.getId()));
    }
}
