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
import com.flowci.core.job.dao.JobAgentDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobPriorityDao;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.event.JobDeletedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.service.LocalTaskService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.domain.ObjectWrapper;
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
import com.google.common.collect.Sets;
import groovy.util.ScriptException;
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
import java.util.concurrent.CountDownLatch;
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

    private static final long RetryInterval = 10 * 1000; // 10 seconds

    private static final int DefaultJobLockTimeout = 20; // seconds

    private static final String FetchAgentLockKey = "fetch-agent";

    @Autowired
    private Path repoDir;

    @Autowired
    private Path tmpDir;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobPriorityDao jobPriorityDao;

    @Autowired
    private JobAgentDao jobAgentDao;

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

    @Autowired
    private StateMachine<JobSmContext> sm;

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

            sm.addHookActionOnTargetStatus(new ActionOnFinishStatus(), Success, Failure, Timeout, Cancelled);
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
        if (job.isDone()) {
            return;
        }
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
        sm.add(PendingToCreated, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                String yml = context.yml;

                setupJobYamlAndSteps(job, yml);
                setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
            }
        });

        sm.add(PendingToLoading, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.LOADING, null);

                context.yml = fetchYamlFromGit(job);
                sm.execute(Loading, Created, context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(Loading, Failure, context);
            }
        });

        sm.add(PendingToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                context.setError(new Exception("cancelled while pending"));
            }
        });
    }

    private void fromLoading() {
        sm.add(LoadingToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(LoadingToCreated, new Action<JobSmContext>() {
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
        sm.add(CreatedToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                context.setError(new Exception("expired before enqueue"));
                log.debug("[Job: Timeout] {} has expired", job.getKey());
            }
        });

        sm.add(CreatedToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(CreatedToQueued, new Action<JobSmContext>() {

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
                sm.execute(context.getCurrent(), Failure, context);
            }
        });
    }

    private void fromQueued() {
        sm.add(QueuedToTimeout, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(QueuedToCancelled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                context.setError(new Exception("cancelled from queue"));
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(QueuedToRunning, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.job;
                eventManager.publish(new JobReceivedEvent(this, job));

                jobPriorityDao.addJob(job.getFlowId(), job.getBuildNumber());
                if (!waitIfJobNotOnTopPriority(job)) {
                    return;
                }

                job.setStartAt(new Date());
                setJobStatusAndSave(job, Job.Status.RUNNING, null);

                // start from root path, and block current thread since don't send ack back to queue
                NodeTree tree = ymlManager.getTree(job);
                CountDownLatch latch = new CountDownLatch(1);
                executeJob(job, Lists.newArrayList(tree.getRoot()), latch);
                latch.await();
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(Queued, Failure, context);
            }
        });

        sm.add(QueuedToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;

                // set current step to exception
                for (String path : job.getCurrentPath()) {
                    Step step = stepService.get(job.getId(), path);
                    stepService.toStatus(step, Step.Status.EXCEPTION, null, false);
                }
            }
        });
    }

    private void fromRunning() {
        sm.add(RunningToRunning, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.job;
                Step step = context.step;

                NodeTree tree = ymlManager.getTree(job);
                Node node = tree.get(step.getNodePath());

                if (node.isLastChildOfParent()) {
                    releaseAgentFromJob(context.job, node, step);
                    releaseAgentToPool(context.job, node, step);
                }

                if (toNextStep(context.job, context.step)) {
                    return;
                }

                toFinishStatus(context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(context.getCurrent(), Failure, context);
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });

        // do not lock job since it will be called from RunningToRunning status
        sm.add(RunningToSuccess, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                logInfo(job, "finished with status {}", Success);
            }
        });

        sm.add(RunningToTimeout, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setRestStepsToSkipped(job);
                sendKillCmdToAllAgents(job);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });

        // failure from job end or exception
        // do not lock job since it will be called from RunningToRunning status
        sm.add(RunningToFailure, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                Step step = context.step;
                stepService.toStatus(step, Step.Status.EXCEPTION, null, false);
                logInfo(job, "finished with status {}", Failure);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });

        sm.add(RunningToCancelling, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.CANCELLING, null);
                sendKillCmdToAllAgents(job);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                sm.execute(context.getCurrent(), Cancelled, context);
            }
        });

        sm.add(RunningToCanceled, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;

                JobAgent jobAgent = getJobAgent(job.getId());
                for (Agent agent : agentService.list(jobAgent.all())) {
                    if (agent.isBusy()) {
                        sm.execute(context.getCurrent(), Cancelling, context);
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

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });
    }

    private void fromCancelling() {
        sm.add(CancellingToCancelled, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

            @Override
            public void accept(JobSmContext context) {
                Job job = context.job;
                setRestStepsToSkipped(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, null);
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });
    }

    private boolean lockJobBefore(JobSmContext context) {
        Job job = context.job;

        Optional<InterLock> lock = lock(job.getId(), "LockJobBefore");

        if (!lock.isPresent()) {
            toFailureStatus(context.job, context.step, new CIException("Fail to lock job"));
            return false;
        }

        context.lock = lock.get();
        context.job = reload(job.getId());
        log.debug("Job {} is locked", job.getId());
        return true;
    }

    private void unlockJobAfter(JobSmContext context) {
        Job job = context.job;
        InterLock lock = context.lock;
        unlock(lock, job.getId());
    }

    private boolean waitIfJobNotOnTopPriority(Job job) {
        while (true) {
            if (job.isExpired()) {
                on(job, Job.Status.TIMEOUT, (c) -> c.setError(new Exception("time out while queueing")));
                return false;
            }

            if (job.isCancelling() || job.isDone()) {
                return false;
            }

            long topPriorityBuildNumber = jobPriorityDao.findMinBuildNumber(job.getFlowId());
            if (job.getBuildNumber() <= topPriorityBuildNumber) {
                return true;
            }

            ThreadHelper.sleep(RetryInterval);
            job = reload(job.getId());
            log.debug("Job {}/{} wait since not on top priority", job.getFlowName(), job.getBuildNumber());
        }
    }

    private Optional<Agent> fetchAgentFromJob(Job job, Node node) {
        FlowNode flow = node.getParentFlowNode();
        Selector selector = flow.fetchSelector();
        JobAgent agents = getJobAgent(job.getId());

        // find agent that can be used directly
        Optional<String> id = agents.getAgent(flow);
        if (id.isPresent()) {
            Agent agent = agentService.get(id.get());
            log.debug("Reuse agent {} directly", agent.getId());
            return Optional.of(agent);
        }

        // find candidate agents within job agent
        List<String> candidates = agents.getCandidates(node);
        Iterable<Agent> list = agentService.list(candidates);
        for (Agent candidate : list) {
            if (candidate.match(selector)) {
                // sync to current job instance and save to db
                jobAgentDao.addFlowToAgent(job.getId(), candidate.getId(), flow.getPathAsString());
                log.debug("Reuse agent {} from candidate", candidate.getId());
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private Optional<Agent> fetchAgentFromPool(Job job, Node node) {
        FlowNode flow = node.getParentFlowNode();
        Selector selector = flow.fetchSelector();

        // find agent outside job, blocking thread
        Optional<Agent> optional = agentService.acquire(job, selector);
        if (optional.isPresent()) {
            Agent agent = optional.get();
            jobAgentDao.addFlowToAgent(job.getId(), agent.getId(), flow.getPathAsString());
            return optional;
        }

        return Optional.empty();
    }

    private void releaseAgentFromJob(Job job, Node node, Step step) {
        String agentId = step.getAgentId();
        FlowNode flow = node.getParentFlowNode();
        jobAgentDao.removeFlowFromAgent(job.getId(), agentId, flow.getPathAsString());
    }

    private void releaseAgentToPool(Job job, Node node, Step step) {
        JobAgent jobAgent = getJobAgent(job.getId());
        String agentId = step.getAgentId();

        if (jobAgent.isOccupiedByFlow(agentId)) {
            return;
        }

        NodeTree tree = ymlManager.getTree(job);
        Selector currentSelector = node.getParentFlowNode().fetchSelector();

        // find selectors of pending steps
        List<Step> notStartedSteps = stepService.list(job, Executed.WaitingStatus);
        Set<Selector> selectors = new HashSet<>(tree.getSelectors().size());
        for (Step s : notStartedSteps) {
            Node n = tree.get(s.getNodePath());
            Selector selector = n.getParentFlowNode().getSelector();
            selectors.add(selector);
        }

        // keep agent for job
        if (selectors.contains(currentSelector)) {
            return;
        }

        // release agent and put it back to pool
        execAgentOperation(
                () -> toFailureStatus(job, step, new CIException("Unable to acquire lock")),
                () -> {
                    agentService.release(Sets.newHashSet(agentId));
                    jobAgentDao.removeAgent(job.getId(), agentId);
                });
    }

    private void toFailureStatus(Job job, Step step, CIException e) {
        on(job, Job.Status.FAILURE, (c) -> {
            c.step = step;
            c.setError(e);
        });
    }

    private void setupJobYamlAndSteps(Job job, String yml) {
        ymlManager.create(job, yml);
        stepService.init(job);
        localTaskService.init(job);

        FlowNode root = YmlParser.load(yml);

        job.setCurrentPathFromNodes(root);
        job.getContext().merge(root.getEnvironments(), false);
    }

    private void sendKillCmdToAllAgents(Job job) {
        JobAgent jobAgent = getJobAgent(job.getId());
        for (Agent agent : agentService.list(jobAgent.all())) {
            if (agent.isOnline()) {
                CmdIn killCmd = cmdManager.createKillCmd();
                agentService.dispatch(killCmd, agent);
            }
        }
    }

    private void setRestStepsToSkipped(Job job) {
        List<Step> steps = stepService.list(job);
        steps.removeIf(step -> !step.isOngoing());
        stepService.toStatus(steps, Step.Status.SKIPPED, null);
    }

    private void on(Job job, Job.Status target, Consumer<JobSmContext> configContext) {
        Status current = new Status(job.getStatus().name());
        Status to = new Status(target.name());

        JobSmContext context = new JobSmContext();
        context.job = job;

        if (configContext != null) {
            configContext.accept(context);
        }

        sm.execute(current, to, context);
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
    private boolean toNextStep(Job job, Step step) throws ScriptException {
        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(NodePath.create(step.getNodePath())); // current node

        stepService.resultUpdate(step);
        log.debug("Step {} been recorded", step.getNodePath());

        // update job attributes and context
        updateJobContextAndLatestStatus(job, node, step);
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        // return if current step is failure
        if (!step.isSuccess()) {
            log.debug("Job {} stop on {}", job.getId(), step.getNodePath());
            return false;
        }

        List<Node> next = node.getNext();
        if (next.isEmpty()) {
            Collection<Node> ends = Sets.newHashSet(tree.ends());
            ends.remove(node); // do not check current node

            Set<Executed.Status> status = getStepsStatus(job, ends);
            return !Collections.disjoint(status, Executed.OngoingStatus);
        }

        // check prev steps status
        Set<Executed.Status> previous = getStepsStatus(job, tree.prevs(next));
        boolean hasFailure = !Collections.disjoint(previous, Executed.FailureStatus);
        boolean hasOngoing = !Collections.disjoint(previous, Executed.OngoingStatus);
        if (hasFailure) {
            return false;
        }

        // do not execute next
        if (hasOngoing) {
            return true;
        }

        executeJob(job, next, null);
        return true;
    }

    /**
     * Get status set from nodes
     *
     * @param job
     * @param nodes
     * @return
     */
    private Set<Executed.Status> getStepsStatus(Job job, Collection<Node> nodes) {
        Set<Executed.Status> status = new HashSet<>(nodes.size());
        for (Node node : nodes) {
            Step step = stepService.get(job.getId(), node.getPathAsString());
            if (step.isSuccess()) {
                status.add(Executed.Status.SUCCESS);
                continue;
            }
            status.add(step.getStatus());
        }
        return status;
    }

    private void executeJob(Job job, List<Node> nodes, CountDownLatch latch) throws ScriptException {
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
                executeJob(job, next, latch);
                continue;
            }

            // skip current node cmd dispatch if the node has children
            if (node.hasChildren()) {
                executeJob(job, node.getNext(), latch);
                continue;
            }

            stepService.toStatus(step, Executed.Status.WAITING_AGENT, null, false);
            findAgentAndDispatch(tree, job, node, step, latch);
        }
    }

    private void findAgentAndDispatch(NodeTree tree, Job instance, Node node, Step step, CountDownLatch latch) {
        String jobId = instance.getId();
        String flowId = instance.getFlowId();

        ThreadPoolTaskExecutor executor = pool.get(jobId);
        if (executor == null) {
            executor = ThreadHelper.createTaskExecutor(tree.getMaxHeight(), 1, 0, jobId);
            pool.put(jobId, executor);
        }

        executor.execute(() -> {
            while (true) {
                ObjectWrapper<Boolean> isBreak = new ObjectWrapper<>(false);
                execAgentOperation(
                        () -> toFailureStatus(instance, step, new CIException("Unable to acquire lock")),
                        () -> {
                            Job job = reload(jobId);

                            if (job.isExpired()) {
                                on(job, Job.Status.TIMEOUT, (c) -> c.setError(new Exception("agent not found within timeout")));
                                isBreak.setValue(true);
                                return;
                            }

                            if (job.isCancelling() || job.isDone()) {
                                isBreak.setValue(true);
                                return;
                            }

                            // find agent from job within current thread
                            Optional<Agent> optional = fetchAgentFromJob(instance, node);
                            if (optional.isPresent()) {
                                Agent agent = optional.get();
                                dispatch(instance, node, step, agent);
                                isBreak.setValue(true);
                                return;
                            }

                            optional = fetchAgentFromPool(job, node);
                            if (optional.isPresent()) {
                                Agent agent = optional.get();

                                job.addAgentSnapshot(agent);
                                setJobStatusAndSave(job, Job.Status.RUNNING, null);

                                dispatch(job, node, step, agent);
                                isBreak.setValue(true);
                                return;
                            }

                            log.debug("Unable to get agent for job {}/{} - {}",
                                    job.getFlowName(), job.getBuildNumber(), node.getPathAsString());
                        }
                );

                if (isBreak.getValue()) {
                    break;
                }

                AcquireLock lock = acquireLocks.computeIfAbsent(flowId, s -> new AcquireLock());
                if (lock.stop) {
                    acquireLocks.remove(flowId);
                    break;
                }

                ThreadHelper.wait(lock, RetryInterval);
            }

            if (latch != null) {
                latch.countDown();
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

        sm.execute(context.getCurrent(), new Status(statusFromContext.name()), context);
    }

    private void execAgentOperation(Runnable onLockNotFound, Runnable onAgentLock) {
        Optional<InterLock> lock = lock(FetchAgentLockKey, "fetch agent");
        if (!lock.isPresent()) {
            onLockNotFound.run();
            return;
        }

        try {
            onAgentLock.run();
        } finally {
            unlock(lock.get(), FetchAgentLockKey);
        }
    }

    private void setJobStatusAndSave(Job job, Job.Status newStatus, String message) {
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

    private JobAgent getJobAgent(String jobId) {
        Optional<JobAgent> optional = jobAgentDao.findById(jobId);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new StatusException("Cannot get agents instance for job " + jobId);
    }

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }

    private Job reload(String jobId) {
        return jobDao.findById(jobId).get();
    }

    private Optional<InterLock> lock(String key, String message) {
        String path = zk.makePath("/job-locks", key);
        Optional<InterLock> lock = zk.lock(path, DefaultJobLockTimeout);
        lock.ifPresent(interLock -> log.debug("Lock: {} - {}", key, message));
        return lock;
    }

    private void unlock(InterLock lock, String key) {
        try {
            zk.release(lock);
            log.debug("Unlock: {}", key);
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
            jobPriorityDao.removeJob(job.getFlowId(), job.getBuildNumber());
            pool.remove(job.getId());

            execAgentOperation(
                    () -> {
                    },

                    // release agent
                    () -> {
                        JobAgent agents = getJobAgent(job.getId());
                        agentService.release(agents.all());
                        for (String agentId : agents.all()) {
                            log.debug("------ [RELEASE] job {}/{} , agent {}",
                                    job.getFlowName(), job.getBuildNumber(), agentId);
                        }
                    }
            );

            localTaskService.executeAsync(job);
        }
    }

    private static class AcquireLock {

        public boolean stop = false;
    }
}
