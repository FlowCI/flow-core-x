package com.flowci.core.job.service;

import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.domain.Agent;
import com.flowci.exception.CIException;
import com.flowci.exception.StatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TtyServiceImpl implements TtyService {

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public void open(String jobId) {
        try {
            Job job = jobService.get(jobId);
            if (job.isDone()) {
                throw new StatusException("Cannot open tty since job is done");
            }

            Agent agent = agentService.get(job.getAgentId());
            if (!agent.isBusy()) {
                throw new StatusException("Cannot open tty since agent not available");
            }

            TtyCmd.In open = new TtyCmd.In()
                    .setId(jobId)
                    .setAction(TtyCmd.Action.OPEN);

            // dispatch cmd to agent, and response will be handled in JobEventService
            agentService.dispatch(open, agent);
        } catch (CIException e) {
            TtyCmd.Out out = new TtyCmd.Out()
                    .setId(jobId)
                    .setAction(TtyCmd.Action.OPEN)
                    .setSuccess(false)
                    .setError(e.getMessage());
            eventManager.publish(new TtyStatusUpdateEvent(this, out));
        }
    }
}
