package com.flowci.core.job.service;

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.domain.Notification;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.task.domain.LocalDockerTask;
import com.flowci.core.task.event.StartAsyncLocalTaskEvent;
import com.flowci.domain.*;
import com.flowci.exception.CIException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.StatusException;
import com.flowci.sm.*;
import com.flowci.tree.*;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
@Service
public class JobActionServiceImpl implements JobActionService {

    private static final Status Pending = new Status(Job.Status.PENDING.name());
    private static final Status Created = new Status(Job.Status.CREATED.name());
    private static final Status Loading = new Status(Job.Status.LOADING.name());
    private static final Status Cancelled = new Status(Job.Status.CANCELLED.name());
    private static final Status Cancelling = new Status(Job.Status.CANCELLING.name());
    private static final Status Queued = new Status(Job.Status.QUEUED.name());
    private static final Status Running = new Status(Job.Status.RUNNING.name());
    private static final Status Timeout = new Status(Job.Status.TIMEOUT.name());
    private static final Status Failure = new Status(Job.Status.FAILURE.name());
    private static final Status Success = new Status(Job.Status.SUCCESS.name());

    // pending
    private static final Transition PendingToLoading = new Transition(Pending, Loading);
    private static final Transition PendingToCreated = new Transition(Pending, Created);

    // loading
    private static final Transition LoadingToFailure = new Transition(Loading, Failure);
    private static final Transition LoadingToCreated = new Transition(Loading, Created);

    // created
    private static final Transition CreatedToQueued = new Transition(Created, Queued);
    private static final Transition CreatedToTimeout = new Transition(Created, Timeout);
    private static final Transition CreatedToFailure = new Transition(Created, Failure);

    // queued
    private static final Transition QueuedToCancelled = new Transition(Queued, Cancelled);
    private static final Transition QueuedToRunning = new Transition(Queued, Running);
    private static final Transition QueuedToTimeout = new Transition(Queued, Timeout);
    private static final Transition QueuedToFailure = new Transition(Queued, Failure);

    // running
    private static final Transition RunningToRunning = new Transition(Running, Running);
    private static final Transition RunningToSuccess = new Transition(Running, Success);
    private static final Transition RunningToCancelling = new Transition(Running, Cancelling);
    private static final Transition RunningToCanceled = new Transition(Running, Cancelled);
    private static final Transition RunningToTimeout = new Transition(Running, Timeout);
    private static final Transition RunningToFailure = new Transition(Running, Failure);

    // cancelling
    private static final Transition CancellingToCancelled = new Transition(Cancelling, Cancelled);

    private static final StateMachine<JobSmContext> Sm = new StateMachine<>("JOB_STATUS");

    @Autowired
    private Path repoDir;

    @Autowired
    private Path tmpDir;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private RabbitOperations jobsQueueManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private SecretService secretService;

    @EventListener
    public void init(ContextRefreshedEvent ignore) {
        fromPending();
        fromLoading();
        fromCreated();
        fromQueued();
        fromRunning();
        fromCancelling();

        Sm.addHookActionOnTargetStatus(context -> {
            Job job = context.job;
            if (hasJobLock(job.getId())) {
                throw new StatusException("Unable to cancel right now, try later");
            }
        }, Cancelled);

        // run local notification task
        Sm.addHookActionOnTargetStatus(notificationConsumer(), Success, Failure, Timeout, Cancelled);
    }

    @Override
    public void toLoading(Job job) {
        on(job, Job.Status.LOADING, null);
    }

    @Override
    public void toCreated(Job job, String yml) {
        on(job, Job.Status.CREATED, context -> {
            context.yml = yml;
        });
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
        if (job.isCancelling()) {
            on(job, Job.Status.CANCELLED, (context) -> context.step = step);
            return;
        }

        on(job, Job.Status.RUNNING, (context) -> context.step = step);
    }

    @Override
    public void toCancelled(Job job, String reason) {
        on(job, Job.Status.CANCELLED, context -> {
            context.reasonForCancel = reason;
        });
    }

    @Override
    public void toTimeout(Job job) {
        on(job, Job.Status.TIMEOUT, null);
    }

    private void fromPending() {
        Sm.add(PendingToCreated, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                String yml = context.yml;

                setupYamlAndSteps(job, yml);
                setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
            }
        });

        Sm.add(PendingToLoading, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.LOADING, null);

                context.yml = fetchYamlFromGit(job);
                Sm.execute(Loading, Created, context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                Sm.execute(Loading, Failure, context);
            }
        });
    }

    private void fromLoading() {
        Sm.add(LoadingToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Throwable err = context.getError();
                setJobStatusAndSave(job, Job.Status.FAILURE, err.getMessage());
            }
        });

        Sm.add(LoadingToCreated, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                String yml = context.yml;

                setupYamlAndSteps(job, yml);
                setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
            }
        });
    }

    private void fromCreated() {
        Sm.add(CreatedToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired before enqueue");
                log.debug("[Job: Timeout] {} has expired", job.getKey());
            }
        });

        Sm.add(CreatedToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Throwable err = context.getError();
                setJobStatusAndSave(job, Job.Status.FAILURE, err.getMessage());
            }
        });

        Sm.add(CreatedToQueued, new Action<JobSmContext>() {

            @Override
            public boolean canRun(JobSmContext context) {
                Job job = context.job;

                if (job.isExpired()) {
                    Sm.execute(context.getCurrent(), Timeout, context);
                    return false;
                }

                return true;
            }

            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.QUEUED, null);

                String queue = job.getQueueName();
                byte[] payload = job.getId().getBytes();

                jobsQueueManager.send(queue, payload, job.getPriority(), job.getExpire());
                logInfo(job, "enqueue");
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(new CIException("Unable to enqueue"));
                Sm.execute(context.getCurrent(), Failure, context);
            }
        });
    }

    private void fromQueued() {
        Function<String, Boolean> canAcquireAgent = (jobId) -> {
            ObjectWrapper<Job> out = new ObjectWrapper<>();
            boolean canContinue = canContinue(jobId, out);
            Job job = out.getValue();

            if (job.isExpired()) {
                JobSmContext context = new JobSmContext();
                context.setJob(job);
                context.setError(new CIException("expired while waiting for agent"));
                Sm.execute(Queued, Timeout, context);
                return false;
            }

            return canContinue;
        };

        Sm.add(QueuedToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Throwable err = context.getError();
                setJobStatusAndSave(job, Job.Status.TIMEOUT, err.getMessage());
            }
        });

        Sm.add(QueuedToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancelled while queued up");
            }
        });

        Sm.add(QueuedToRunning, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                eventManager.publish(new JobReceivedEvent(this, job));

                Optional<Agent> available = agentService.acquire(job, canAcquireAgent);

                if (available.isPresent()) {
                    Agent agent = available.get();
                    context.agentId = agent.getId();
                    dispatch(job, agent);
                }
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                Sm.execute(Queued, Failure, context);
            }
        });

        Sm.add(QueuedToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                String agentId = context.getAgentId();
                Throwable e = context.getError();

                log.debug("Fail to dispatch job {} to agent {}", job.getId(), agentId, e);

                // set current step to exception
                String jobId = job.getId();
                String nodePath = job.getCurrentPath(); // set in the dispatch
                stepService.toStatus(jobId, nodePath, ExecutedCmd.Status.EXCEPTION, null);

                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
                agentService.tryRelease(agentId);
            }
        });
    }

    private void fromRunning() {
        Sm.add(RunningToRunning, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                Job job = context.job;
                Optional<InterLock> lock = lockJob(job.getId());

                if (!lock.isPresent()) {
                    log.debug("Fail to lock job {}", job.getId());
                    context.setError(new CIException("Unexpected status"));
                    Sm.execute(context.getCurrent(), Failure, context);
                    return false;
                }

                context.lock = lock.get();
                return true;
            }

            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                log.debug("Job {} is locked", job.getId());

                // refresh job after lock
                context.job = jobDao.findById(job.getId()).get();

                if (toNextStep(context)) {
                    return;
                }

                toFinishStatus(context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                context.setError(e);
                log.debug("Fail to dispatch job {} to agent {}", job.getId(), job.getAgentId(), e);
                Sm.execute(context.getCurrent(), Failure, context);
            }

            @Override
            public void onFinally(JobSmContext context) {
                Job job = context.job;
                InterLock lock = context.getLock();
                releaseLock(lock, job.getId());
            }
        });

        Sm.add(RunningToSuccess, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                agentService.tryRelease(job.getAgentId());
                logInfo(job, "finished with status {}", Success);

                setJobStatusAndSave(job, Job.Status.SUCCESS, null);
            }
        });

        Sm.add(RunningToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Agent agent = agentService.get(job.getAgentId());

                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);

                if (agent.isOnline()) {
                    CmdIn killCmd = cmdManager.createKillCmd();
                    agentService.dispatch(killCmd, agent);
                    agentService.tryRelease(agent.getId());
                }
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
            }
        });

        // failure from job end or exception
        Sm.add(RunningToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                ExecutedCmd step = context.step;
                Throwable err = context.getError();

                if (!step.isAfter()) {
                    stepService.toStatus(job.getId(), step.getNodePath(), ExecutedCmd.Status.EXCEPTION, null);
                }

                Agent agent = agentService.get(job.getAgentId());
                agentService.tryRelease(agent.getId());

                logInfo(job, "finished with status {}", Failure);
                setJobStatusAndSave(job, Job.Status.FAILURE, err.getMessage());
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });

        Sm.add(RunningToCancelling, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Agent agent = agentService.get(job.getAgentId());

                CmdIn killCmd = cmdManager.createKillCmd();
                setJobStatusAndSave(job, Job.Status.CANCELLING, null);

                agentService.dispatch(killCmd, agent);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Sm.execute(context.getCurrent(), Cancelled, context);
            }
        });

        Sm.add(RunningToCanceled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                String reason = context.reasonForCancel;
                Agent agent = agentService.get(job.getAgentId());

                if (agent.isOnline()) {
                    Sm.execute(context.getCurrent(), Cancelling, context);
                    return;
                }

                agentService.tryRelease(agent.getId());
                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, reason);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, e.getMessage());
            }
        });
    }

    private void fromCancelling() {
        Sm.add(CancellingToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // run after steps
                if (toNextStep(context)) {
                    return;
                }

                Job job = context.job;
                Agent agent = agentService.get(job.getAgentId());
                agentService.tryRelease(agent.getId());

                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, null);
            }
        });
    }

    private void setupYamlAndSteps(Job job, String yml) {
        FlowNode root = YmlParser.load(job.getFlowName(), yml);

        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());
        job.getContext().merge(root.getEnvironments(), false);

        ymlManager.create(job, yml);
        stepService.init(job);
    }

    private void setRestStepsToSkipped(Job job) {
        List<ExecutedCmd> steps = stepService.list(job);
        for (ExecutedCmd step : steps) {
            if (step.isRunning() || step.isPending()) {
                stepService.toStatus(step, ExecutedCmd.Status.SKIPPED, null);
            }
        }
    }

    private void on(Job job, Job.Status target, Consumer<JobSmContext> configContext) {
        Status current = new Status(job.getStatus().name());
        Status to = new Status(target.name());

        JobSmContext context = new JobSmContext().setJob(job);

        if (configContext != null) {
            configContext.accept(context);
        }

        Sm.execute(current, to, context);
    }

    private String fetchYamlFromGit(Job job) {
        final String gitUrl = job.getGitUrl();

        if (!StringHelper.hasValue(gitUrl)) {
            throw new NotAvailableException("Git url is missing");
        }

        final Path dir = getFlowRepoDir(gitUrl, job.getYamlRepoBranch());

        try {
            GitClient client = new GitClient(gitUrl, tmpDir, getSimpleSecret(job.getCredentialName()));
            client.klone(dir, job.getYamlRepoBranch());
        } catch (Exception e) {
            throw new NotAvailableException("Unable to fetch yaml config for flow");
        }

        String[] files = dir.toFile().list((currentDir, fileName) ->
                (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) && fileName.startsWith(".flowci"));

        if (files == null || files.length == 0) {
            throw new NotAvailableException("Unable to find yaml file in repo");
        }

        try {
            byte[] ymlInBytes = Files.readAllBytes(Paths.get(dir.toString(), files[0]));
            return new String(ymlInBytes);
        } catch (IOException e) {
            throw new NotAvailableException("Unable to read yaml file in repo").setExtra(job);
        }
    }

    /**
     * Get flow repo path: {repo dir}/{flow id}
     */
    private Path getFlowRepoDir(String repoUrl, String branch) {
        String b64 = Base64.getEncoder().encodeToString(repoUrl.getBytes());
        return Paths.get(repoDir.toString(), b64 + "_" + branch);
    }

    private SimpleSecret getSimpleSecret(String credentialName) {
        if (Strings.isNullOrEmpty(credentialName)) {
            return null;
        }

        final Secret secret = secretService.get(credentialName);
        return secret.toSimpleSecret();
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
        ExecutedCmd nextCmd = stepService.toStatus(job.getId(), nextPath, ExecutedCmd.Status.RUNNING, null);

        // dispatch job to agent queue
        CmdIn cmd = cmdManager.createShellCmd(job, next, nextCmd);
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", next.getName(), agent.getName());
    }

    /**
     * Dispatch next step to agent
     *
     * @return true if next step dispatched, false if no more steps
     */
    private boolean toNextStep(JobSmContext context) {
        Job job = context.job;
        ExecutedCmd step = context.step;

        // save executed cmd
        stepService.resultUpdate(step);
        log.debug("Step {} been recorded", step);

        NodePath currentPath = NodePath.create(step.getNodePath());

        // verify job node path is match cmd node path
        if (!currentPath.equals(NodePath.create(job.getCurrentPath()))) {
            log.error("Invalid executed cmd callback: does not match job current node path");
            return false;
        }

        NodeTree tree = ymlManager.getTree(job);
        StepNode node = tree.get(currentPath);
        updateJobTime(job, step, tree, node);
        updateJobStatusAndContext(job, node, step);

        // to next step
        Optional<StepNode> next = findNext(tree, node, step.isSuccess());
        if (next.isPresent()) {
            String nextPath = next.get().getPathAsString();
            job.setCurrentPath(nextPath);

            ExecutedCmd nextCmd = stepService.toStatus(job.getId(), nextPath, ExecutedCmd.Status.RUNNING, null);
            context.setStep(nextCmd);

            Agent agent = agentService.get(job.getAgentId());
            CmdIn cmd = cmdManager.createShellCmd(job, next.get(), nextCmd);
            setJobStatusAndSave(job, Job.Status.RUNNING, null);

            agentService.dispatch(cmd, agent);
            logInfo(job, "send to agent: step={}, agent={}", next.get().getName(), agent.getName());
            return true;
        }

        return false;
    }

    private void toFinishStatus(JobSmContext context) {
        Job job = context.job;

        Job.Status statusFromContext = job.getStatusFromContext();
        String error = job.getErrorFromContext();
        ObjectsHelper.ifNotNull(error, s -> context.setError(new CIException(s)));

        Sm.execute(context.getCurrent(), new Status(statusFromContext.name()), context);
    }

    private synchronized void setJobStatusAndSave(Job job, Job.Status newStatus, String message) {
        // check status order, just save job if new status is downgrade
        if (job.getStatus().getOrder() >= newStatus.getOrder()) {
            jobDao.save(job);
            return;
        }

        job.setStatus(newStatus);
        job.setMessage(message);
        job.setStatusToContext(newStatus);

        jobDao.save(job);
        eventManager.publish(new JobStatusChangeEvent(this, job));
        logInfo(job, "status = {}", job.getStatus());
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
            job.setErrorToContext(cmd.getError());
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

    private boolean canContinue(String jobId, ObjectWrapper<Job> out) {
        Job job = jobDao.findById(jobId).get();
        out.setValue(job);

        if (job.isExpired()) {
            return false;
        }

        if (job.isCancelling()) {
            return false;
        }

        return !job.isDone();
    }

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }

    private Optional<InterLock> lockJob(String jobId) {
        String path = zk.makePath("/job-locks", jobId);
        return zk.lock(path, 10);
    }

    private boolean hasJobLock(String jobId) {
        String path = zk.makePath("/job-locks", jobId);
        return zk.exist(path);
    }

    private void releaseLock(InterLock lock, String jobId) {
        try {
            zk.release(lock);
            log.debug("Job {} is released", jobId);
        } catch (Exception warn) {
            log.warn(warn);
        }
    }

    private Consumer<JobSmContext> notificationConsumer() {
        return context -> {
            Job job = context.job;
            for (Notification n : job.getNotifications()) {
                if (!n.isEnabled()) {
                    continue;
                }

                StringVars input = new StringVars(job.getContext());
                input.merge(n.getInputs());

                LocalDockerTask task = new LocalDockerTask();
                task.setName(n.getPlugin()); // plugin name as task name
                task.setPlugin(n.getPlugin());
                task.setJobId(job.getId());
                task.setInputs(input);

                eventManager.publish(new StartAsyncLocalTaskEvent(this, task));
            }
        };
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class JobSmContext extends Context {

        private Job job;

        public String yml;

        private ExecutedCmd step;

        private String agentId;

        private String reasonForCancel;

        private InterLock lock;
    }
}
