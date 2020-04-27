package com.flowci.core.job.service;

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.domain.Agent;
import com.flowci.domain.CmdIn;
import com.flowci.exception.NotFoundException;
import com.flowci.sm.Context;
import com.flowci.sm.StateMachine;
import com.flowci.sm.Status;
import com.flowci.sm.Transition;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Log4j2
@Service
public class JobActionServiceImpl implements JobActionService {

    private static final Status Created = new Status(Job.Status.CREATED.name());
    private static final Status Canceled = new Status(Job.Status.CANCELLED.name());
    private static final Status Queued = new Status(Job.Status.QUEUED.name());
    private static final Status Running = new Status(Job.Status.RUNNING.name());

    // start
    private static final Transition CreatedToQueued = new Transition(Created, Queued);

    // cancel
    private static final Transition QueuedToCancel = new Transition(Queued, Canceled);
    private static final Transition RunningToCancel = new Transition(Running, Canceled);

    private static final StateMachine Sm = new StateMachine("JOB_STATUS");

    @Autowired
    private JobDao jobDao;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private AgentService agentService;

    @EventListener
    public void init(ContextRefreshedEvent ignore) {
        onStart();
        onCancel();
    }

    @Override
    public void start(Job job) {
        on(job, Job.Status.QUEUED);
    }

    @Override
    public void cancel(Job job) {
        on(job, Job.Status.CANCELLED);
    }

    private void onStart() {
        Sm.add(CreatedToQueued, context -> {
            Job job = (Job) context.get("job");

            if (job.isExpired()) {
                setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired before enqueue");
                log.debug("[Job: Timeout] {} has expired", job.getKey());
                return;
            }

            try {
                RabbitQueueOperation manager = flowJobQueueManager.get(job.getQueueName());
                setJobStatusAndSave(job, Job.Status.QUEUED, null);
                manager.send(job.getId().getBytes(), job.getPriority(), job.getExpire());
                logInfo(job, "enqueue");
            } catch (Throwable e) {
                setJobStatusAndSave(job, Job.Status.FAILURE, "Unable to enqueue");
            }
        });
    }

    private void onCancel() {
        Sm.add(QueuedToCancel, (context) -> {
            Job job = (Job) context.get("job");
            setJobStatusAndSave(job, Job.Status.CANCELLED, "canceled while queued up");
        });

        Sm.add(RunningToCancel, (context) -> {
            Job job = (Job) context.get("job");

            try {
                Agent agent = agentService.get(job.getAgentId());

                if (agent.isOnline()) {
                    CmdIn killCmd = cmdManager.createKillCmd();
                    agentService.dispatch(killCmd, agent);
                    logInfo(job, " cancel cmd been send to {}", agent.getName());
                    setJobStatusAndSave(job, Job.Status.CANCELLING, null);
                    return;
                }

                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while agent offline");
            } catch (NotFoundException e) {
                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while not agent assigned");
            }
        });
    }

    private void on(Job job, Job.Status target) {
        Status current = new Status(job.getStatus().name());

        Context context = new Context();
        context.put("job", job);

        Sm.execute(current, new Status(target.name()), context);
    }

    @Override
    public synchronized Job setJobStatusAndSave(Job job, Job.Status newStatus, String message) {
        if (job.getStatus() == newStatus) {
            return jobDao.save(job);
        }

        if (Job.FINISH_STATUS.contains(newStatus)) {
            if (Objects.isNull(job.getFinishAt())) {
                job.setFinishAt(new Date());
            }
        }

        job.setStatus(newStatus);
        job.setMessage(message);
        job.getContext().put(Variables.Job.Status, newStatus.name());
        jobDao.save(job);
        eventManager.publish(new JobStatusChangeEvent(this, job));

        log.debug("Job status {} = {}", job.getId(), job.getStatus());
        return job;
    }

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }
}
