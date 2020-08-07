package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.service.LocalTaskService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.domain.Agent;
import com.flowci.domain.SimpleSecret;
import com.flowci.domain.Vars;
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
import groovy.util.ScriptException;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
@Service
public class JobActionManagerImpl implements JobActionManager {

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
    private ConditionManager conditionManager;

    @Autowired
    private LocalTaskService localTaskService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private SecretService secretService;

    @EventListener
    public void init(ContextRefreshedEvent ignore) {
        try {
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
        } catch (SmException.TransitionExisted ignored) {
        }
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
    public void toContinue(Job job, Step step) {
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

                setupJobYamlAndSteps(job, yml);
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

                setupJobYamlAndSteps(job, yml);
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
            Job job = jobDao.findById(jobId).get();

            if (job.isExpired()) {
                JobSmContext context = new JobSmContext();
                context.setJob(job);
                Sm.execute(Queued, Timeout, context);
                return false;
            }

            if (job.isCancelling()) {
                return false;
            }

            return !job.isDone();
        };

        Sm.add(QueuedToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
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
            public void accept(JobSmContext context) throws Exception {
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
                stepService.toStatus(jobId, nodePath, Step.Status.EXCEPTION, null);

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
            public void accept(JobSmContext context) throws Exception {
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
                Step step = context.step;
                Throwable err = context.getError();

                stepService.toStatus(job.getId(), step.getNodePath(), Step.Status.EXCEPTION, null);

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
                Job job = context.job;
                Agent agent = agentService.get(job.getAgentId());
                agentService.tryRelease(agent.getId());

                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, null);
            }
        });
    }

    private void setupJobYamlAndSteps(Job job, String yml) {
        FlowNode root = YmlParser.load(job.getFlowName(), yml);

        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());
        job.getContext().merge(root.getEnvironments(), false);

        ymlManager.create(job, yml);
        stepService.init(job);
        localTaskService.init(job);
    }

    private void setRestStepsToSkipped(Job job) {
        List<Step> steps = stepService.list(job);
        for (Step step : steps) {
            if (step.isRunning() || step.isPending()) {
                stepService.toStatus(step, Step.Status.SKIPPED, null);
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
    private void dispatch(Job job, Agent agent) throws ScriptException {
        job.setAgentId(agent.getId());
        job.setAgentSnapshot(agent);

        NodeTree tree = ymlManager.getTree(job);
        StepNode next = tree.next(tree.getRoot().getPath());
        execute(job, next);
    }

    /**
     * Dispatch next step to agent
     *
     * @return true if next step dispatched, false if no more steps or failure
     */
    private boolean toNextStep(JobSmContext context) throws ScriptException {
        Job job = context.job;
        Step step = context.step; // current step

        NodeTree tree = ymlManager.getTree(job);
        StepNode node = tree.get(NodePath.create(step.getNodePath())); // current node

        stepService.resultUpdate(step);
        log.debug("Step {} been recorded", step);

        // verify job node path is match cmd node path
        if (!node.getPath().equals(NodePath.create(job.getCurrentPath()))) {
            log.error("Invalid executed cmd callback: does not match job current node path");
            return false;
        }

        updateJobTime(job, step, tree, node);
        updateJobContextAndLatestStatus(job, node, step);

        Optional<StepNode> next = findNext(tree, node, step.isSuccess());
        if (next.isPresent()) {
            return execute(job, next.get());
        }

        return false;
    }

    private boolean execute(Job job, StepNode node) throws ScriptException {
        // check null that indicate no node can be executed
        if (Objects.isNull(node)) {
            return false;
        }

        NodeTree tree = ymlManager.getTree(job);
        job.setCurrentPath(node.getPathAsString());
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        Step step = stepService.get(job.getId(), node.getPathAsString());
        CmdIn cmd = cmdManager.createShellCmd(job, step, tree);
        boolean canExecute = conditionManager.run(cmd);

        if (!canExecute) {
            setStepToSkipped(node, step);
            updateJobTime(job, step, tree, node);
            setJobStatusAndSave(job, Job.Status.RUNNING, null);

            // set next node again due to skip
            if (node.hasChildren()) {
                StepNode next = tree.nextRootStep(node.getPath());
                return execute(job, next);
            }

            StepNode next = tree.next(node.getPath());
            return execute(job, next);
        }

        // skip group node
        if (node.hasChildren()) {
            StepNode next = tree.next(node.getPath());
            return execute(job, next);
        }

        setJobStatusAndSave(job, Job.Status.RUNNING, null);
        stepService.toStatus(step, Step.Status.RUNNING, null);

        Agent agent = agentService.get(job.getAgentId());
        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
        return true;
    }

    private void setStepToSkipped(StepNode node, Step step) {
        step.setStartAt(new Date());
        step.setFinishAt(new Date());
        stepService.toStatus(step, Step.Status.SKIPPED, Step.MessageSkippedOnCondition);

        for (StepNode subNode : node.getChildren()) {
            Step subStep = stepService.get(step.getJobId(), subNode.getPathAsString());
            subStep.setStartAt(new Date());
            subStep.setFinishAt(new Date());
            stepService.toStatus(subStep, Executed.Status.SKIPPED, Step.MessageSkippedOnCondition);
        }
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
            // push updated job object as well
            eventManager.publish(new JobStatusChangeEvent(this, job));
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

    private void updateJobTime(Job job, Step step, NodeTree tree, StepNode node) {
        if (tree.isFirst(node.getPath())) {
            job.setStartAt(step.getStartAt());
        }
        job.setFinishAt(step.getFinishAt());
    }

    private void updateJobContextAndLatestStatus(Job job, StepNode node, Step step) {
        // merge output to job context
        Vars<String> context = job.getContext();
        context.merge(step.getOutput());

        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());
        context.put(Variables.Job.Steps, stepService.toVarString(job, node));

        job.setStatusToContext(StatusHelper.convert(step));
        job.setErrorToContext(step.getError());
    }

    private Optional<StepNode> findNext(NodeTree tree, Node current, boolean isSuccess) {
        StepNode next = tree.next(current.getPath());

        if (Objects.isNull(next) || !isSuccess) {
            return Optional.empty();
        }

        return Optional.of(next);
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
            localTaskService.executeAsync(job);
        };
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class JobSmContext extends Context {

        private Job job;

        public String yml;

        private Step step;

        private String agentId;

        private String reasonForCancel;

        private InterLock lock;
    }
}
