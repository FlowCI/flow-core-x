package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.event.AgentIdleEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobAgents;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.JobDeletedEvent;
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

    private static final long RetryIntervalOnNotFound = 10 * 1000; // 10 seconds

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

    // flow id, notify lock for agent
    private final Map<String, AcquireLock> acquireLocks = new ConcurrentHashMap<>();

    // job node execute thread pool
    private final Map<String, ThreadPoolTaskExecutor> pool = new ConcurrentHashMap<>();

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

            Sm.addHookActionOnTargetStatus(new ActionOnFinishStatus(), Success, Failure, Timeout, Cancelled);
        } catch (SmException.TransitionExisted ignored) {
        }
    }

    @EventListener
    public void doNotifyToFindAgent(AgentIdleEvent event) {
        Agent agent = event.getAgent();
        Optional<Job> optional = jobDao.findById(agent.getId());
        if (optional.isPresent()) {
            String flowId = optional.get().getFlowId();
            acquireLocks.computeIfPresent(flowId, (s, lock) -> {
                ThreadHelper.notifyAll(lock);
                return lock;
            });
        }
    }

    @EventListener
    public void stopJobsThatWaitingForAgent(JobDeletedEvent event) {
        AcquireLock lock = acquireLocks.get(event.getFlow().getId());
        if (Objects.isNull(lock)) {
            return;
        }

        lock.stop = true;
        ThreadHelper.notifyAll(lock);
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
            context.setError(new CIException(reason));
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
                context.setError(new Exception("cancelled while pending"));
            }
        });
    }

    private void fromLoading() {
        Sm.add(LoadingToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
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
                context.setError(new Exception("expired before enqueue"));
                log.debug("[Job: Timeout] {} has expired", job.getKey());
            }
        });

        Sm.add(CreatedToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
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
                // handled on ActionOnFinishStatus
            }
        });

        Sm.add(QueuedToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                context.setError(new Exception("cancelled from queue"));
                // handled on ActionOnFinishStatus
            }
        });

        Sm.add(QueuedToRunning, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.job;
                eventManager.publish(new JobReceivedEvent(this, job));
                NodeTree tree = ymlManager.getTree(job);

                ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(tree.getMaxHeight(), 1, 0, "job-exec-");
                pool.putIfAbsent(job.getId(), executor);

                setJobStatusAndSave(job, Job.Status.RUNNING, null);

                // start from root path
                executeJob(job, Lists.newArrayList(tree.getRoot()));
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

                // set current step to exception
                for (String path : job.getCurrentPath()) {
                    Step step = stepService.get(job.getId(), path);
                    stepService.toStatus(step, Step.Status.EXCEPTION, null, false);
                }
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
                log.debug("Fail to dispatch job {} to agent {}", job.getId(), job.agentIds(), e);
                Sm.execute(context.getCurrent(), Failure, context);
            }

            @Override
            public void onFinally(JobSmContext context) {
                Job job = context.job;
                InterLock lock = context.getLock();
                unlockJob(lock, job.getId());
            }
        });

        Sm.add(RunningToSuccess, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                logInfo(job, "finished with status {}", Success);
            }
        });

        Sm.add(RunningToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;

                setRestStepsToSkipped(job);

                for (Agent agent : agentService.list(job.agentIds())) {
                    if (agent.isOnline()) {
                        CmdIn killCmd = cmdManager.createKillCmd();
                        agentService.dispatch(killCmd, agent);
                    }
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
                stepService.toStatus(step, Step.Status.EXCEPTION, null, false);
                logInfo(job, "finished with status {}", Failure);
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

                for (Agent agent : agentService.list(job.agentIds())) {
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

                for (Agent agent : agentService.list(job.agentIds())) {
                    if (agent.isBusy()) {
                        Sm.execute(context.getCurrent(), Cancelling, context);
                        return;
                    }
                }

                setRestStepsToSkipped(job);
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
                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, null);
            }
        });
    }

    /**
     * Fetch agent for job
     *
     * @return Optional agent, do not need to handle when optional is empty
     */
    private Optional<Agent> fetchAgent(Job job, Node node) {
        JobAgents jobAgents = job.getAgents();
        FlowNode flow = node.getParentFlowNode();

        // find agent that can be used directly
        Optional<String> id = jobAgents.getAgent(flow);
        if (id.isPresent()) {
            Agent agent = agentService.get(id.get());
            return Optional.of(agent);
        }

        // find candidate agents within job agent
        List<String> candidates = jobAgents.getCandidates(node);
        Iterable<Agent> list = agentService.list(candidates);
        for (Agent candidate : list) {
            if (candidate.match(flow.getSelector())) {
                jobAgents.save(candidate.getId(), flow);
                return Optional.of(candidate);
            }
        }

        // find agent outside job, blocking thread
        Optional<Agent> optional = agentService.acquire(job, flow.getSelector());
        if (optional.isPresent()) {
            Agent agent = optional.get();
            jobAgents.save(agent.getId(), flow);
            job.addAgentSnapshot(agent);
            return optional;
        }

        return Optional.empty();
    }

    private void releaseAgent(Job job, Node node, Step step) {
        if (!node.isLastChildOfParent()) {
            return;
        }

        FlowNode flow = node.getParentFlowNode();
        job.getAgents().remove(step.getAgentId(), flow);

        // TODO: release agent outside job if cannot reused for other flows
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
        steps.removeIf(step -> !step.isOngoing());
        stepService.toStatus(steps, Step.Status.SKIPPED, null);
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
     * Dispatch next step to agent, job will be saved on final function of Running status
     *
     * @return true if next step dispatched or have to wait for previous steps, false if no more steps or failure
     */
    private boolean toNextStep(JobSmContext context) throws ScriptException {
        Job job = context.job;
        Step step = context.step; // current step

        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(NodePath.create(step.getNodePath())); // current node

        stepService.resultUpdate(step);
        log.debug("Step {} been recorded", step);

        // update job attributes and context
        updateJobContextAndLatestStatus(job, node, step);
        releaseAgent(job, node, step);
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        List<Node> next = node.getNext();
        if (next.isEmpty() || !step.isSuccess()) {
            return false;
        }

        // check prev steps status
        Set<Executed.Status> previous = getPreviousStepsStatus(job, next);
        boolean hasFailure = !Collections.disjoint(previous, Executed.FailureStatus);
        boolean hasOngoing = !Collections.disjoint(previous, Executed.OngoingStatus);
        if (hasFailure) {
            return false;
        }

        // do not execute next
        if (hasOngoing) {
            return true;
        }

        executeJob(job, next);
        return true;
    }

    private Set<Executed.Status> getPreviousStepsStatus(Job job, List<Node> next) {
        List<Node> list = new LinkedList<>();
        for (Node n : next) {
            list.addAll(n.getPrev());
        }

        Set<Executed.Status> status = new HashSet<>(list.size());

        for (Node prev : list) {
            Step step = stepService.get(job.getId(), prev.getPathAsString());

            // return if any one prev steps failed
            if (!step.isOngoing() && !step.isSuccess()) {
                status.add(Executed.Status.EXCEPTION);
                return status;
            }

            status.add(step.getStatus());
        }

        return status;
    }

    private void executeJob(Job job, List<Node> nodes) throws ScriptException {
        job.setCurrentPathFromNodes(nodes);
        setJobStatusAndSave(job, job.getStatus(), null);

        NodeTree tree = ymlManager.getTree(job);

        for (Node node : nodes) {
            boolean condition = runCondition(job, node);
            Step step = stepService.get(job.getId(), node.getPathAsString());

            if (!condition) {
                setSkipStatusToStep(step);
                updateJobContextAndLatestStatus(job, node, step);

                List<Node> next = tree.skip(node.getPath());
                executeJob(job, next);
                continue;
            }

            // skip current node cmd dispatch if the node has children
            if (node.hasChildren()) {
                List<Node> next = tree.next(node.getPath());
                executeJob(job, next);
                continue;
            }

            stepService.toStatus(step, Executed.Status.WAITING_AGENT, null, false);
            waitAgentAndDispatch(job, node, step);
        }
    }

    private void waitAgentAndDispatch(Job instance, Node node, Step step) {
        String jobId = instance.getId();
        String flowId = instance.getFlowId();

        ThreadPoolTaskExecutor executor = pool.get(jobId);
        executor.execute(() -> {
            while (true) {

                // keep sync on job with multiple subflow, ensure job read/save is thread safe
                synchronized (executor) {
                    Job job = jobDao.findById(jobId).get();

                    if (job.isExpired()) {
                        on(job, Job.Status.TIMEOUT, (c) -> {
                            c.setError(new Exception("agent not found within timeout"));
                        });
                        return;
                    }

                    if (job.isCancelling() || job.isDone()) {
                        return;
                    }

                    Optional<Agent> optional = fetchAgent(job, node);
                    if (optional.isPresent()) {
                        setJobStatusAndSave(job, Job.Status.RUNNING, null);
                        Agent agent = optional.get();
                        dispatch(job, node, step, agent);
                        return;
                    }

                    log.debug("Unable to fetch agent for job {} - {}", job.getId(), node.getPathAsString());
                }

                AcquireLock lock = acquireLocks.computeIfAbsent(flowId, s -> new AcquireLock());
                if (lock.stop) {
                    acquireLocks.remove(flowId);
                    return;
                }

                ThreadHelper.wait(lock, RetryIntervalOnNotFound);
            }
        });
    }

    /**
     * Run condition script and return is ran successfully
     */
    private boolean runCondition(Job job, Node node) throws ScriptException {
        boolean shouldRun = true;
        if (job.getTrigger() == Job.Trigger.MANUAL || job.getTrigger() == Job.Trigger.API) {
            if (node.getPath().isRoot()) {
                shouldRun = false;
            }
        }

        if (!shouldRun) {
            return true;
        }

        Vars<String> inputs = node.fetchEnvs().merge(job.getContext());
        return conditionManager.run(node.getCondition(), inputs);
    }

    private void dispatch(Job job, Node node, Step step, Agent agent) {
        step.setAgentId(agent.getId());
        stepService.toStatus(step, Step.Status.RUNNING, null, false);

        ShellIn cmd = cmdManager.createShellCmd(job, step, node);
        agentService.dispatch(cmd, agent);
        logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
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
        job.setFinishAt(step.getFinishAt());

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

    private void unlockJob(InterLock lock, String jobId) {
        try {
            zk.release(lock);
            log.debug("Job {} is released", jobId);
        } catch (Exception warn) {
            log.warn(warn);
        }
    }

    private class ActionOnFinishStatus implements Consumer<JobSmContext> {

        @Override
        public void accept(JobSmContext context) {
            Job job = context.job;

            // save job with status
            Throwable error = context.getError();
            String message = error == null ? "" : error.getMessage();
            setJobStatusAndSave(job, context.getTargetToJobStatus(), message);

            // release agent
            agentService.tryRelease(job.agentIds());

            // run notification task
            localTaskService.executeAsync(job);
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class JobSmContext extends Context {

        private Job job;

        public String yml;

        private Step step;

        private InterLock lock;

        public Job.Status getTargetToJobStatus() {
            String name = this.to.getName();
            return Job.Status.valueOf(name);
        }
    }

    private static class AcquireLock {

        public boolean stop = false;
    }
}
