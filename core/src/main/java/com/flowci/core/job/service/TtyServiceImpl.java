/*
 * Copyright 2020 flow.ci
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

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.common.exception.CIException;
import com.flowci.common.exception.StatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TtyServiceImpl implements TtyService {

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private StepService stepService;

    @Override
    public void execute(TtyCmd.In in) {
        try {
            Agent agent = getAgent(in.getId(), in.getNodePath());

            // dispatch cmd to agent,
            // response will be handled in JobEventService
            // std out/err log handled in LoggingService
            agentService.dispatch(in, agent);
        } catch (CIException e) {
            TtyCmd.Out out = new TtyCmd.Out()
                    .setId(in.getId())
                    .setAction(in.getAction())
                    .setSuccess(false)
                    .setError(e.getMessage());
            eventManager.publish(new TtyStatusUpdateEvent(this, out));
        }
    }

    private Agent getAgent(String jobId, String nodePath) {
        Job job = jobService.get(jobId);
        if (job.isDone()) {
            throw new StatusException("Cannot open tty since job is done");
        }

        Step step = stepService.get(jobId, nodePath);
        Agent agent = agentService.get(step.getAgentId());
        if (!agent.isBusy()) {
            throw new StatusException("Cannot open tty since agent not available");
        }

        return agent;
    }
}
