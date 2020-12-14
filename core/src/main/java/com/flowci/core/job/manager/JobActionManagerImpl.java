package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.service.LocalTaskService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
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
import com.google.common.collect.Lists;
import groovy.util.ScriptException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final Transition PendingToCancelled = new Transition(Pending, Cancelled);

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

    private final Map<String, ThreadPoolTaskExecutor> agentFetchExecutor = new ConcurrentHashMap<>();

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

            // Clean up agent fetch executor for job
            Sm.addHookActionOnTargetStatus(context -> {
                Job job = context.job;
                ThreadPoolTaskExecutor executor = agentFetchExecutor.get(job.getId());
                executor.shutdown();
                agentFetchExecutor.remove(job.getId());
            }, Success, Failure, Timeout, Cancelled);

            // run local notification task
            Sm.addHookActionOnTargetStatus(context -> {
                Job job = context.job;
                localTaskService.executeAsync(job);
            }, Success, Failure, Timeout, Cancelled);
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

        Sm.add(PendingToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancelled while pending");
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
                NodeTree tree = ymlManager.getTree(job);

                // init thread pool for agent fetching
                int maxHeight = tree.getMaxHeight();
                agentFetchExecutor.put(job.getId(), ThreadHelper.createTaskExecutor(maxHeight, 1, 0, "agent-fetch-"));

                // start from root path
                saveJobAndExecute(job, Lists.newArrayList(tree.getRoot()), true);
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
                Throwable e = context.getError();

                // set current step to exception
                for (String path : job.getCurrentPath()) {
                    Step step = stepService.get(job.getId(), path);
                    stepService.toStatus(step, Step.Status.EXCEPTION, null, false);
                }

                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
                agentService.tryRelease(job.getAgents().values());
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
                log.debug("Fail to dispatch job {} to agent {}", job.getId(), job.getAgents(), e);
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
                agentService.tryRelease(job.getAgents().values());
                logInfo(job, "finished with status {}", Success);

                setJobStatusAndSave(job, Job.Status.SUCCESS, null);
            }
        });

        Sm.add(RunningToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;

                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);

                for (Agent agent : agentService.list(job.getAgents().values())) {
                    if (agent.isOnline()) {
                        CmdIn killCmd = cmdManager.createKillCmd();
                        agentService.dispatch(killCmd, agent);
                    }
                }

                agentService.tryRelease(job.getAgents().values());
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

                stepService.toStatus(step, Step.Status.EXCEPTION, null, false);

                agentService.tryRelease(job.getAgents().values());

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

                for (Agent agent : agentService.list(job.getAgents().values())) {
                    CmdIn killCmd = cmdManager.createKillCmd();
                    agentService.dispatch(killCmd, agent);
                }

                setJobStatusAndSave(job, Job.Status.CANCELLING, null);
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

                for (Agent agent : agentService.list(job.getAgents().values())) {
                    if (agent.isBusy()) {
                        Sm.execute(context.getCurrent(), Cancelling, context);
                        return;
                    }
                }

                agentService.tryRelease(job.getAgents().values());
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
                agentService.tryRelease(job.getAgents().values());

                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, null);
            }
        });
    }

    private Optional<Agent> fetchAgent(Job job, FlowNode flow) {
        Function<String, Boolean> canAcquireAgent = (jobId) -> {
            Job reloaded = jobDao.findById(jobId).get();

            if (reloaded.isExpired()) {
                JobSmContext context = new JobSmContext();
                context.setJob(reloaded);
                Sm.execute(Queued, Timeout, context);
                return false;
            }

            if (reloaded.isCancelling()) {
                return false;
            }

            return !reloaded.isDone();
        };

        return agentService.acquire(job, flow, canAcquireAgent);
    }

    private void setupJobYamlAndSteps(Job job, String yml) {
        ymlManager.create(job, yml);
        stepService.init(job);
        localTaskService.init(job);

        FlowNode root = YmlParser.load(yml);

        job.setCurrentPathFromNodes(root);
        job.getContext().merge(root.getEnvironments(), false);
    }

    private void setRestStepsToSkipped(Job job) {
        List<Step> steps = stepService.list(job);
        for (Step step : steps) {
            if (step.isRunning() || step.isPending()) {
                stepService.toStatus(step, Step.Status.SKIPPED, null, false);
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
     * Dispatch next step to agent
     *
     * @return true if next step dispatched, false if no more steps or failure
     */
    private boolean toNextStep(JobSmContext context) throws ScriptException {
        Job job = context.job;
        Step step = context.step; // current step

        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(NodePath.create(step.getNodePath())); // current node

        stepService.resultUpdate(step);
        log.debug("Step {} been recorded", step);

        job.setFinishAt(step.getFinishAt());
        updateJobContextAndLatestStatus(job, node, step);

        List<Node> next = node.getNext();
        if (next.isEmpty() || !step.isSuccess()) {
            setJobStatusAndSave(job, job.getStatusFromContext(), null);
            return false;
        }

        return saveJobAndExecute(job, next, false);
    }

    /**
     * Execute job on target nodes
     *
     * @param job   target job
     * @param nodes the nodes will be executed
     * @return
     * @throws ScriptException
     */
    private boolean saveJobAndExecute(Job job, List<Node> nodes, boolean fromRoot) throws ScriptException {
        job.setCurrentPathFromNodes(nodes);

        // do not update job status when run from root
        Job.Status newStatus = Job.Status.RUNNING;
        if (fromRoot) {
            newStatus = job.getStatus();
        }

        setJobStatusAndSave(job, newStatus, null);

        // TODO: not support parallel yet
        for (Node node : nodes) {
            if (!(node instanceof FlowNode)) {
                return execute(job, node);
            }

            FlowNode f = (FlowNode) node;
            ThreadPoolTaskExecutor executor = agentFetchExecutor.get(job.getId());

            executor.execute(() -> {
                Optional<Agent> optional = fetchAgent(job, f);

                // save agent data to job
                if (optional.isPresent()) {
                    Agent agent = optional.get();
                    job.addAgent(f, agent.getId());
                    job.addAgentSnapshot(agent);
                    setJobStatusAndSave(job, Job.Status.RUNNING, null);

                    try {
                        execute(job, node);
                    } catch (ScriptException e) {
                        // to failure status
                        this.on(job, Job.Status.FAILURE, (c) -> c.setError(e));
                    }
                }
            });

            return true;
        }

        return false;
    }

    private boolean execute(Job job, Node node) throws ScriptException {
        NodeTree tree = ymlManager.getTree(job);
        Step step = stepService.get(job.getId(), node.getPathAsString());

        // check execute condition
        if (canStartConditionCheck(job, node)) {
            Vars<String> inputs = node.fetchEnvs().merge(job.getContext());
            boolean canExecute = conditionManager.run(node.getCondition(), inputs);

            // skip current node if condition not match
            if (!canExecute) {
                setSkipStatusToStep(step);

                job.setFinishAt(step.getFinishAt());

                List<Node> next = tree.skip(node.getPath());
                return saveJobAndExecute(job, next, false);
            }
        }

        // skip current node cmd dispatch if the node has children
        if (node.hasChildren()) {
            List<Node> next = tree.next(node.getPath());
            return saveJobAndExecute(job, next, false);
        }

        stepService.toStatus(step, Step.Status.RUNNING, null, false);

        String parentFlowPath = node.getParentFlowNode().getPathAsString();
        String agentId = job.getAgents().get(parentFlowPath);

        if (agentId == null) {
            throw new StatusException("cannot get agent id on executing");
        }

        Agent agent = agentService.get(agentId);
        ShellIn cmd = cmdManager.createShellCmd(job, step, tree);
        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
        return true;
    }

    private boolean canStartConditionCheck(Job job, Node node) {
        if (job.getTrigger() == Job.Trigger.MANUAL || job.getTrigger() == Job.Trigger.API) {
            if (node.getPath().isRoot()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Skip step and all children
     */
    private void setSkipStatusToStep(Step step) {
        step.setStartAt(new Date());
        step.setFinishAt(new Date());
        stepService.toStatus(step, Step.Status.SKIPPED, Step.MessageSkippedOnCondition, true);
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

    private void updateJobContextAndLatestStatus(Job job, Node node, Step step) {
        // merge output to job context
        Vars<String> context = job.getContext();
        context.merge(step.getOutput());

        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());
        context.put(Variables.Job.Steps, stepService.toVarString(job, node));

        // DO NOT update job status from context
        job.setStatusToContext(StatusHelper.convert(step));
        job.setErrorToContext(step.getError());
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

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class JobSmContext extends Context {

        private Job job;

        public String yml;

        private Step step;

        private String reasonForCancel;

        private InterLock lock;
    }
}
