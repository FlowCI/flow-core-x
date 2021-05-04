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
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.CmdOut;
import com.flowci.core.agent.domain.ShellOut;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.OnCmdOutEvent;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.Errors;
import com.flowci.tree.FlowNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
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
    private JobActionService jobActionService;

    @Autowired
    private ConditionManager conditionManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private JobService jobService;

    @Autowired
    private StepService stepService;

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
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
        Flow flow = event.getFlow();
        String queue = flow.getQueueName();
        jobsQueueManager.removeConsumer(queue);
        jobService.delete(event.getFlow());
    }

    @EventListener
    public void startNewJob(CreateNewJobEvent event) {
        appTaskExecutor.execute(() -> {
            try {
                FlowNode root = ymlManager.parse(event.getYml());
                boolean canCreateJob = true;

                if (root.hasCondition()) {
                    root.getEnvironments().merge(event.getInput());
                    canCreateJob = conditionManager.run(root.getCondition(), root.getEnvironments());
                }

                if (!canCreateJob) {
                    log.info("Unable to create job of flow {} since condition not match", event.getFlow().getName());
                    return;
                }

                Job job = jobService.create(event.getFlow(), event.getYml(), event.getTrigger(), event.getInput());
                jobService.start(job);

            } catch (Throwable e) {
                log.warn(e);
            }
        });
    }

    @EventListener
    public void updateJobAndStepWhenOffline(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (!agent.hasJob()) {
            return;
        }

        if (!agent.isOffline()) {
            return;
        }

        jobActionService.toCancelled(agent.getJobId(), Errors.AgentOffline);
    }

    @EventListener
    public void handleCmdOutFromAgent(OnCmdOutEvent event) {
        byte[] raw = event.getRaw();
        byte ind = raw[0];
        byte[] body = Arrays.copyOfRange(raw, 1, raw.length);

        try {
            switch (ind) {
                case CmdOut.ShellOutInd:
                    ShellOut shellOut = objectMapper.readValue(body, ShellOut.class);

                    Step step = stepService.get(shellOut.getId());
                    step.setFrom(shellOut);
                    stepService.resultUpdate(step);

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
    }

    @Override
    public void handleCallback(Step step) {
        jobActionService.toContinue(step);
    }

    //====================================================================
    //        %% Init events
    //====================================================================

    @EventListener(value = ContextRefreshedEvent.class)
    public void startJobDeadLetterConsumer() throws IOException {
        String deadLetterQueue = rabbitProperties.getJobDlQueue();
        jobsQueueManager.startConsumer(deadLetterQueue, true, (header, body, envelope) -> {
            String jobId = new String(body);
            try {
                jobActionService.toTimeout(jobId);
            } catch (Exception e) {
                log.warn(e);
            }
            return false;
        }, null);
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

            jobsQueueManager.startConsumer(queue, false, (header, body, envelope) -> {
                try {
                    String jobId = new String(body);
                    jobActionService.toRun(jobId);
                } catch (Exception e) {
                    log.warn(e);
                }
                return true;
            }, appTaskExecutor);
        } catch (IOException e) {
            log.warn(e);
        }
    }
}
