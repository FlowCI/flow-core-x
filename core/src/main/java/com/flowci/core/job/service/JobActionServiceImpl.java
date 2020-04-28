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
import com.flowci.core.job.util.StatusHelper;
import com.flowci.domain.Agent;
import com.flowci.domain.CmdIn;
import com.flowci.domain.Vars;
import com.flowci.exception.NotFoundException;
import com.flowci.sm.Context;
import com.flowci.sm.StateMachine;
import com.flowci.sm.Status;
import com.flowci.sm.Transition;
import com.flowci.tree.Node;
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
import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
@Service
public class JobActionServiceImpl implements JobActionService {

    private static final Status Created = new Status(Job.Status.CREATED.name());
    private static final Status Canceled = new Status(Job.Status.CANCELLED.name());
    private static final Status Cancelling = new Status(Job.Status.CANCELLING.name());
    private static final Status Queued = new Status(Job.Status.QUEUED.name());
    private static final Status Running = new Status(Job.Status.RUNNING.name());
    private static final Status Timeout = new Status(Job.Status.TIMEOUT.name());
    private static final Status Failure = new Status(Job.Status.FAILURE.name());
    private static final Status Success = new Status(Job.Status.SUCCESS.name());

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
    private static final Transition RunningToRunning = new Transition(Running, Running);
    private static final Transition RunningToSuccess = new Transition(Running, Success);
    private static final Transition RunningToCancelling = new Transition(Running, Cancelling);
    private static final Transition RunningToCanceled = new Transition(Running, Canceled);
    private static final Transition RunningToTimeout = new Transition(Running, Timeout);
    private static final Transition RunningToFailure = new Transition(Running, Failure);

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
    public void toStart(Job job) {
        on(job, Job.Status.QUEUED, null);
    }

    @Override
    public void toRun(Job job) {
        on(job, Job.Status.RUNNING, null);
    }

    @Override
    public void toContinue(Job job, ExecutedCmd step) {
        on(job, Job.Status.RUNNING, (context) -> {
            context.put("step", step);
        });
    }

    @Override
    public void toCancel(Job job) {
        on(job, Job.Status.CANCELLED, null);
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
        job.setStatusToContext(newStatus);

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
                setJobStatusAndSave(job, Job.Status.QUEUED, null);

                manager.send(job.getId().getBytes(), job.getPriority(), job.getExpire());
                logInfo(job, "enqueue");
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
        Sm.add(RunningToRunning, (context) -> {
            Job job = getJob(context);
            ExecutedCmd execCmd = (ExecutedCmd) context.get("step");

            // save executed cmd
            stepService.resultUpdate(execCmd);
            log.debug("Executed cmd {} been recorded", execCmd);

            NodePath currentPath = NodePath.create(execCmd.getNodePath());

            // verify job node path is match cmd node path
            if (!currentPath.equals(NodePath.create(job.getCurrentPath()))) {
                log.error("Invalid executed cmd callback: does not match job agent node path");
                return;
            }

            NodeTree tree = ymlManager.getTree(job);
            StepNode node = tree.get(currentPath);
            updateJobTime(job, execCmd, tree, node);
            updateJobStatusAndContext(job, node, execCmd);

            Agent agent = agentService.get(job.getAgentId());
            context.put("agent", agent);

            Optional<StepNode> next = findNext(tree, node, execCmd.isSuccess());

            if (next.isPresent()) {
                String nextPath = next.get().getPathAsString();
                job.setCurrentPath(nextPath);
                ExecutedCmd nextCmd = stepService.statusChange(job.getId(), nextPath, ExecutedCmd.Status.RUNNING, null);

                try {
                    CmdIn cmd = cmdManager.createShellCmd(job, node, nextCmd);
                    setJobStatusAndSave(job, Job.Status.RUNNING, null);

                    agentService.dispatch(cmd, agent);
                    logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
                    return;
                } catch (Throwable e) {
                    log.debug("Fail to dispatch job {} to agent {}", job.getId(), agent.getId(), e);

                    context.put("error", e.getMessage());
                    context.put("step", nextCmd);
                    Sm.execute(getCurrent(context), Failure, context);
                }
            }

            // to finish status
            Job.Status statusFromContext = job.getStatusFromContext();
            Sm.execute(getCurrent(context), new Status(statusFromContext.name()), context);
        });

        Sm.add(RunningToSuccess, (context) -> {
            Job job = getJob(context);
            Agent agent = (Agent) context.get("agent");

            agentService.tryRelease(agent.getId());
            logInfo(job, "finished with status {}", Success);

            setJobStatusAndSave(job, Job.Status.SUCCESS, null);
        });

        // failure from job end or exception
        Sm.add(RunningToFailure, (context) -> {
            Job job = getJob(context);
            Agent agent = (Agent) context.get("agent");
            ExecutedCmd step = (ExecutedCmd) context.get("step");
            String error = (String) context.get("error");

            if (Objects.isNull(error)) {
                error = step.getError();
            }

            stepService.statusChange(job.getId(), step.getNodePath(), ExecutedCmd.Status.EXCEPTION, null);
            agentService.tryRelease(agent.getId());
            logInfo(job, "finished with status {}", Failure);

            setJobStatusAndSave(job, Job.Status.FAILURE, error);
        });

        Sm.add(RunningToCancelling, (context) -> {
            Job job = getJob(context);
            Agent agent = (Agent) context.get("agent");

            CmdIn killCmd = cmdManager.createKillCmd();
            agentService.dispatch(killCmd, agent);
            logInfo(job, " cancel cmd been send to {}", agent.getName());
            setJobStatusAndSave(job, Job.Status.CANCELLING, null);
        });

        Sm.add(RunningToCanceled, (context) -> {
            Job job = getJob(context);

            try {
                Agent agent = agentService.get(job.getAgentId());
                context.put("agent", agent);

                if (agent.isOnline()) {
                    Sm.execute(getCurrent(context), Cancelling, context);
                    return;
                }

                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while agent offline");
            } catch (NotFoundException e) {
                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while not agent assigned");
            }
        });
    }

    private void on(Job job, Job.Status target, Consumer<Context> configContext) {
        Status current = new Status(job.getStatus().name());
        Status to = new Status(target.name());

        Context context = new Context();
        context.put("job", job);

        if (configContext != null) {
            configContext.accept(context);
        }

        Sm.execute(current, to, context);
    }

    /**
     * Dispatch job to agent when Queue to Running
     */
    private void dispatch(Job job, Agent agent) {
        NodeTree tree = ymlManager.getTree(job);
        StepNode next = tree.next(NodePath.create(job.getCurrentPath()));

        // do not accept job without regular steps
        if (Objects.isNull(next)) {
            log.debug("Next node cannot be found when process job {}", job);
            return;
        }

        String nextPath = next.getPathAsString();
        log.debug("Next step of job {} is {}", job.getId(), next.getName());

        // set path, agent id, agent name and status to job
        job.setCurrentPath(nextPath);
        job.setAgentId(agent.getId());
        job.setAgentSnapshot(agent);

        // set executed cmd step to running
        ExecutedCmd nextCmd = stepService.statusChange(job.getId(), nextPath, ExecutedCmd.Status.RUNNING, null);

        // dispatch job to agent queue
        CmdIn cmd = cmdManager.createShellCmd(job, next, nextCmd);
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", next.getName(), agent.getName());
    }

    private void updateJobTime(Job job, ExecutedCmd execCmd, NodeTree tree, StepNode node) {
        if (tree.isFirst(node.getPath())) {
            job.setStartAt(execCmd.getStartAt());
        }
        job.setFinishAt(execCmd.getFinishAt());
    }

    private void updateJobStatusAndContext(Job job, StepNode node, ExecutedCmd cmd) {
        // merge output to job context
        Vars<String> context = job.getContext();
        context.merge(cmd.getOutput());

        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());
        context.put(Variables.Job.Steps, stepService.toVarString(job, node));

        // after status not apart of job status
        if (!node.isAfter()) {
            job.setStatusToContext(StatusHelper.convert(cmd));
        }
    }

    private Optional<StepNode> findNext(NodeTree tree, Node current, boolean isSuccess) {
        StepNode next = tree.next(current.getPath());
        if (Objects.isNull(next)) {
            return Optional.empty();
        }

        // find step from after
        if (!isSuccess && !next.isAfter()) {
            return findNext(tree, next, false);
        }

        return Optional.of(next);
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
