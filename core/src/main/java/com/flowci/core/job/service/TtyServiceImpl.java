package com.flowci.core.job.service;

import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.domain.TtyLog;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.domain.Agent;
import com.flowci.exception.CIException;
import com.flowci.exception.StatusException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Log4j2
@Service
public class TtyServiceImpl implements TtyService {

    @Autowired
    private String ttyLogQueue;

    @Autowired
    private String topicForTtyLogs;

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private RabbitOperations logQueueManager;

    @Autowired
    private SocketPushManager socketPushManager;

    @EventListener(ContextRefreshedEvent.class)
    public void startTtyLog() throws IOException {
        logQueueManager.startConsumer(ttyLogQueue, true, message -> {
            if (!message.hasHeader()) {
                log.warn("Invalid tty log message");
                return true;
            }

            Object id = message.getHeaders().get(TtyLog.ID_HEADER);
            if (Objects.isNull(id)) {
                log.warn("Invalid tty log message");
                return true;
            }

            String topic = topicForTtyLogs + "/" + id.toString();
            socketPushManager.push(topic, message.getBody());
            return true;
        });
    }

    @Override
    public void open(String jobId) {
        dispatch(new TtyCmd.In()
                .setId(jobId)
                .setAction(TtyCmd.Action.OPEN));
    }

    @Override
    public void shell(String jobId, String script) {
        dispatch(new TtyCmd.In()
                .setId(jobId)
                .setAction(TtyCmd.Action.SHELL)
                .setInput(script));
    }

    @Override
    public void close(String jobId) {
        dispatch(new TtyCmd.In()
                .setId(jobId)
                .setAction(TtyCmd.Action.CLOSE));
    }

    private void dispatch(TtyCmd.In in) {
        try {
            Agent agent = getAgent(in.getId());

            // dispatch cmd to agent, and response will be handled in JobEventService
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

    private Agent getAgent(String jobId) {
        Job job = jobService.get(jobId);
        if (job.isDone()) {
            throw new StatusException("Cannot open tty since job is done");
        }

        Agent agent = agentService.get(job.getAgentId());
        if (!agent.isBusy()) {
            throw new StatusException("Cannot open tty since agent not available");
        }

        return agent;
    }
}
