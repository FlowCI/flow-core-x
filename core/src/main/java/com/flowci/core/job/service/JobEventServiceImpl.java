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
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.JobActionEvent;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.Errors;
import com.flowci.tree.FlowNode;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Service
@AllArgsConstructor
public class JobEventServiceImpl implements JobEventService {

    private final ObjectMapper objectMapper;

    private final SpringEventManager eventManager;

    private final JobActionService jobActionService;

    private final ConditionManager conditionManager;

    private final YmlManager ymlManager;

    private final TaskExecutor appTaskExecutor;

    private final JobService jobService;

    private final StepService stepService;

    @EventListener
    public void onFlowInitiated(FlowInitEvent event) {
        for (var f : event.getFlows()) {
            jobService.init(f);
        }
    }

    @EventListener
    public void onFlowCreated(FlowCreatedEvent event) {
        jobService.init(event.getFlow());
    }

    @EventListener
    public void onFlowDeleted(FlowDeletedEvent event) {
        jobService.delete(event.getFlow());
    }

    @EventListener
    public void onJobAction(JobActionEvent event) {
        if (event.isToRun()) {
            jobActionService.toRun(event.getJobId());
            return;
        }

        if (event.isToTimeOut()) {
            jobActionService.toTimeout(event.getJobId());
        }
    }

    @EventListener
    public void startNewJob(CreateNewJobEvent event) {
        appTaskExecutor.execute(() -> {
            try {
                FlowYml ymlEntity = event.getYmlEntity();
                String[] array = FlowYml.toRawArray(ymlEntity.getList());
                FlowNode root = ymlManager.parse(array);
                boolean canCreateJob = true;

                if (root.hasCondition()) {
                    root.getEnvironments().merge(event.getInput());
                    canCreateJob = conditionManager.run(root.getCondition(), root.getEnvironments());
                }

                if (!canCreateJob) {
                    log.info("Unable to create job of flow {} since condition not match", event.getFlow().getName());
                    return;
                }

                Job job = jobService.create(event.getFlow(), ymlEntity.getList(), event.getTrigger(), event.getInput());
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
                    handleCallback(shellOut);
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
    public void handleCallback(ShellOut so) {
        String jobId = stepService.get(so.getId()).getJobId();
        jobActionService.toContinue(jobId, so);
    }
}
