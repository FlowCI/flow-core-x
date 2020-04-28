package com.flowci.core.job.service;

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.domain.Agent;
import com.flowci.domain.CmdIn;
import com.flowci.exception.NotFoundException;
import com.flowci.sm.Context;
import com.flowci.sm.StateMachine;
import com.flowci.sm.Status;
import com.flowci.sm.Transition;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.StepNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Log4j2
@Service
public class JobActionServiceImpl implements JobActionService {

    private static final Status Created = new Status(Job.Status.CREATED.name());
    private static final Status Canceled = new Status(Job.Status.CANCELLED.name());
    private static final Status Queued = new Status(Job.Status.QUEUED.name());
    private static final Status Running = new Status(Job.Status.RUNNING.name());
    private static final Status Timeout = new Status(Job.Status.TIMEOUT.name());
    private static final Status Failure = new Status(Job.Status.FAILURE.name());

    // start
    private static final Transition CreatedToQueued = new Transition(Created, Queued);
    private static final Transition CreatedToTimeout = new Transition(Created, Timeout);
    private static final Transition CreatedToFailure = new Transition(Created, Failure);

    // queued
    private static final Transition QueuedToCancel = new Transition(Queued, Canceled);
    private static final Transition QueuedToRunning = new Transition(Queued, Running);
    private static final Transition QueuedToTimeout = new Transition(Queued, Timeout);
    private static final Transition QueuedToFailure = new Transition(Queued, Failure);

    // running
    private static final Transition RunningToCancel = new Transition(Running, Canceled);

    private static final StateMachine Sm = new StateMachine("JOB_STATUS");

    @Autowired
    private JobDao jobDao;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @EventListener
    public void init(ContextRefreshedEvent ignore) {
        onStart();
        onQueued();
        onRunning();
    }

    @Override
    public void start(Job job) {
        on(job, Job.Status.QUEUED);
    }

    @Override
    public void run(Job job) {
        on(job, Job.Status.RUNNING);
    }

    @Override
    public void cancel(Job job) {
        on(job, Job.Status.CANCELLED);
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

    private void onStart() {
        Sm.add(CreatedToTimeout, context -> {
            Job job = getJob(context);
            setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired before enqueue");
            log.debug("[Job: Timeout] {} has expired", job.getKey());
        });

        Sm.add(CreatedToFailure, context -> {
            Job job = getJob(context);
            String err = (String) context.get(Context.ERROR);
            setJobStatusAndSave(job, Job.Status.FAILURE, err);
        });

        Sm.add(CreatedToQueued, context -> {
            Job job = getJob(context);

            if (job.isExpired()) {
                Sm.execute(getCurrent(context), Timeout, context);
                return;
            }

            try {
                RabbitQueueOperation manager = flowJobQueueManager.get(job.getQueueName());
                manager.send(job.getId().getBytes(), job.getPriority(), job.getExpire());
                logInfo(job, "enqueue");
                setJobStatusAndSave(job, Job.Status.QUEUED, null);
            } catch (Throwable e) {
                context.put(Context.ERROR, "Unable to enqueue");
                Sm.execute(getCurrent(context), Failure, context);
            }
        });
    }

    private void onQueued() {
        Function<String, Boolean> canAcquireAgent = (jobId) -> {
            Optional<Job> optional = jobDao.findById(jobId);
            if (!optional.isPresent()) {
                return false;
            }

            Job job = optional.get();

            if (job.isExpired()) {
                Context context = new Context();
                context.put("job", job);
                context.put(Context.ERROR, "expired while waiting for agent");
                Sm.execute(Queued, Timeout, context);
                return false;
            }

            return !job.isCancelled();
        };

        Sm.add(QueuedToTimeout, context -> {
            Job job = getJob(context);
            String err = (String) context.get(Context.ERROR);
            setJobStatusAndSave(job, Job.Status.TIMEOUT, err);
        });

        Sm.add(QueuedToCancel, (context) -> {
            Job job = getJob(context);
            setJobStatusAndSave(job, Job.Status.CANCELLED, "canceled while queued up");
        });

        Sm.add(QueuedToRunning, (context) -> {
            Job job = getJob(context);
            eventManager.publish(new JobReceivedEvent(this, job));
            Optional<Agent> available = agentService.acquire(job, canAcquireAgent);

            available.ifPresent(agent -> {
                try {
                    dispatch(job, agent);
                    setJobStatusAndSave(job, Job.Status.RUNNING, null);
                } catch (Throwable e) {
                    context.put("agentId", agent.getId());
                    context.put("exception", e);
                    Sm.execute(Queued, Failure, context);
                }
            });
        });

        Sm.add(QueuedToFailure, (context) -> {
            Job job = getJob(context);
            String agentId = (String) context.get("agentId");
            Throwable e = (Throwable) context.get("exception");

            log.debug("Fail to dispatch job {} to agent {}", job.getId(), agentId, e);

            // set current step to exception
            String jobId = job.getId();
            String nodePath = job.getCurrentPath(); // set in the dispatch
            stepService.statusChange(jobId, nodePath, ExecutedCmd.Status.EXCEPTION, null);

            setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            agentService.tryRelease(agentId);
        });
    }

    private void onRunning() {
        Sm.add(RunningToCancel, (context) -> {
            Job job = getJob(context);

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
        Status to = new Status(target.name());

        Context context = new Context();
        context.put("job", job);

        Sm.execute(current, to, context);
    }

    private void dispatch(Job job, Agent agent) {
        NodeTree tree = ymlManager.getTree(job);
        StepNode next = tree.next(NodePath.create(job.getCurrentPath()));

        // do not accept job without regular steps
        if (Objects.isNull(next)) {
            log.debug("Next node cannot be found when process job {}", job);
            return;
        }

        log.debug("Next step of job {} is {}", job.getId(), next.getName());

        // set path, agent id, agent name and status to job
        job.setCurrentPath(next.getPathAsString());
        job.setAgentId(agent.getId());
        job.setAgentSnapshot(agent);

        // set executed cmd step to running
        ExecutedCmd executedCmd = stepService.get(job.getId(), next.getPathAsString());
        if (!executedCmd.isRunning()) {
            stepService.statusChange(job.getId(), next.getPathAsString(), ExecutedCmd.Status.RUNNING, null);
        }

        // dispatch job to agent queue
        CmdIn cmd = cmdManager.createShellCmd(job, next, executedCmd);
        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", next.getName(), agent.getName());
    }

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }

    public static Job getJob(Context context) {
        return (Job) context.get("job");
    }

    public static Status getCurrent(Context context) {
        return (Status) context.get(Context.STATUS_CURRENT);
    }

    public static Status getTo(Context context) {
        return (Status) context.get(Context.STATUS_TO);
    }
}
