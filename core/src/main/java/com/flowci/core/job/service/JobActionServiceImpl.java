/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.job.service;

import com.flowci.common.exception.CIException;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.common.exception.StatusException;
import com.flowci.common.helper.ObjectsHelper;
import com.flowci.common.helper.StringHelper;
import com.flowci.common.sm.*;
import com.flowci.core.agent.domain.*;
import com.flowci.core.agent.event.IdleAgentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.job.dao.JobAgentDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobPriorityDao;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.LockManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.Errors;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.common.domain.SimpleSecret;
import com.flowci.common.domain.Vars;
import com.flowci.tree.*;
import com.flowci.zookeeper.InterLock;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import groovy.util.ScriptException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static com.flowci.core.job.domain.Executed.Status.RUNNING;
import static com.flowci.core.job.domain.Executed.Status.WAITING_AGENT;

@Slf4j
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
    private static final Transition RunningToCancelled = new Transition(Running, Cancelled);
    private static final Transition RunningToTimeout = new Transition(Running, Timeout);
    private static final Transition RunningToFailure = new Transition(Running, Failure);

    // cancelling
    private static final Transition CancellingToCancelled = new Transition(Cancelling, Cancelled);

    private static final long RetryInterval = 10 * 1000; // 10 seconds

    @Autowired
    private Path repoDir;

    @Autowired
    private Path tmpDir;

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
    private LockManager lockManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private SecretService secretService;

    @Autowired
    private StateMachine<JobSmContext> sm;

    @PostConstruct
    public void init() {
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
    public void onIdleAgent(IdleAgentEvent event) {
        String agentId = event.getAgentId();
        event.setFetched(true);

        List<JobKey> keys = jobPriorityDao.findAllMinBuildNumber();
        // TODO: flow priority

        for (JobKey key : keys) {
            if (key.getBuildNumber() == null) {
                continue;
            }

            Optional<Job> optional = jobDao.findByKey(key.toString());
            if (!optional.isPresent()) {
                continue;
            }

            Job job = optional.get();
            if (!job.isRunning()) {
                continue;
            }

            Optional<InterLock> lock = lockManager.lock(job.getId());
            if (lock.isEmpty()) {
                toFailureStatus(job, new CIException("Fail to lock job"));
                continue;
            }

            try {
                NodeTree tree = ymlManager.getTree(job);
                boolean isAssigned = assignAgentToWaitingStep(agentId, job, tree, true);

                if (isAssigned) {
                    event.setFetched(false); // set to false since agent has been assigned
                    return;
                }
            } catch (Exception e) {
                toFailureStatus(job, new CIException(e.getMessage()));
            } finally {
                lockManager.unlock(lock.get(), job.getId());
            }
        }
    }

    @Override
    public void toLoading(String jobId) {
        onTransition(jobId, Loading, null);
    }

    @Override
    public void toCreated(JobYml jobYml) {
        onTransition(jobYml.getId(), Created, context -> context.setYml(jobYml));
    }

    @Override
    public void toStart(String jobId) {
        onTransition(jobId, Queued, null);
    }

    @Override
    public void toRun(String jobId) {
        onTransition(jobId, Running, null);
    }

    @Override
    public void toContinue(String jobId, ShellOut so) {
        onTransition(jobId, Running, c -> {
            Step step = stepService.get(so.getId());
            step.setFrom(so);
            stepService.resultUpdate(step);
            log.info("[Callback]: {}-{} = {}", step.getJobId(), step.getNodePath(), step.getStatus());

            c.setStep(step);
            Job job = c.getJob();
            log.debug("---- Job Status {} {} {} {}",
                    job.isOnPostSteps(),
                    step.getNodePath(),
                    job.getStatus(),
                    JobContextHelper.getStatus(job)
            );

            if (job.isCancelling()) {
                c.setTo(Cancelled);
            }
        });
    }

    @Override
    public void toCancelled(String jobId, CIException exception) {
        onTransition(jobId, Cancelled, context -> {
            context.setError(exception);

            Job job = context.getJob();
            Set<String> currentPath = job.getCurrentPath();

            if (!currentPath.isEmpty()) {
                String nodePath = currentPath.iterator().next();
                context.setStep(stepService.get(job.getId(), nodePath));
            }
        });
    }

    @Override
    public void toTimeout(String jobId) {
        onTransition(jobId, Timeout, null);
    }

    private void fromPending() {
        sm.add(PendingToCreated, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                doFromXToCreated(context);
            }
        });

        sm.add(PendingToLoading, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.LOADING, null);

                context.setYml(fetchYamlFromGit(job));
                sm.execute(Loading, Created, context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(Loading, Failure, context);
            }
        });

        sm.add(PendingToCancelled, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                context.setError(new Exception("cancelled while pending"));
            }
        });
    }

    private void fromLoading() {
        sm.add(LoadingToFailure, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(LoadingToCreated, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                doFromXToCreated(context);
            }
        });
    }

    private void doFromXToCreated(JobSmContext context) {
        Job job = context.getJob();
        JobYml yml = context.getYml();

        setupJobYamlAndSteps(job, yml);
        setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
    }

    private void fromCreated() {
        sm.add(CreatedToTimeout, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                context.setError(new Exception("expired before enqueue"));
                log.debug("[Job: Timeout] {} has expired", job.getKey());
            }
        });

        sm.add(CreatedToFailure, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(CreatedToQueued, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.QUEUED, null);

                String queue = job.getQueueName();
                byte[] payload = job.getId().getBytes();

                jobsQueueManager.publish(queue, payload, job.getPriority(), job.getExpire());
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
        sm.add(QueuedToTimeout, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(QueuedToCancelled, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                context.setError(new Exception("cancelled from queue"));
                // handled on ActionOnFinishStatus
            }
        });

        sm.add(QueuedToRunning, new JobActionBase() {

            @Override
            public boolean canRun(JobSmContext context) {
                return !context.getJob().isDone();
            }

            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.getJob();
                eventManager.publish(new JobReceivedEvent(this, job));

                jobPriorityDao.addJob(job.getFlowId(), job.getBuildNumber());
                if (!waitIfJobNotOnTopPriority(context)) {
                    return;
                }

                job.setStartAt(new Date());
                setJobStatusAndSave(job, Job.Status.RUNNING, null);

                NodeTree tree = ymlManager.getTree(job);

                // start job from job's current step path
                List<Node> nodesToStart = Lists.newLinkedList();
                for (String p : job.getCurrentPath()) {
                    nodesToStart.add(tree.get(p));
                }

                if (nodesToStart.isEmpty()) {
                    nodesToStart.add(tree.getRoot());
                }

                logInfo(job, "QueuedToRunning: start from nodes " + nodesToStart.toString());
                executeJob(job, nodesToStart);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(Queued, Failure, context);
            }
        });

        sm.add(QueuedToFailure, new JobActionBase() {
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
        sm.add(RunningToRunning, new JobActionBase() {
            @Override
            public boolean canRun(JobSmContext context) {
                return !context.getJob().isDone();
            }

            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.getJob();
                Step step = context.getStep();

                updateJobContextAndLatestStatus(job, step);
                setJobStatusAndSave(job, Job.Status.RUNNING, null);
                log.debug("Step {} {} been recorded", step.getNodePath(), step.getStatus());

                if (!step.isSuccess()) {
                    toFinishStatus(context);
                    return;
                }

                if (releaseAgentOrAssignToWaitingStep(job, step)) {
                    return;
                }

                if (toNextStep(job, step)) {
                    return;
                }

                toFinishStatus(context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                context.setError(e);
                sm.execute(context.getCurrent(), Failure, context);
            }
        });

        // do not lock job since it will be called from RunningToRunning status
        sm.add(RunningToSuccess, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                logInfo(job, "finished with status {}", Success);
            }
        });

        sm.add(RunningToTimeout, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                killOngoingSteps(job, job.isOnPostSteps());
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
            }
        });

        // failure from job end or exception
        // do not lock job since it will be called from RunningToRunning status
        sm.add(RunningToFailure, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) throws ScriptException {
                Job job = context.getJob();
                killOngoingSteps(job, job.isOnPostSteps());
                runPostStepsIfNeeded(context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });

        sm.add(RunningToCancelling, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) {
                Job job = context.getJob();
                setJobStatusAndSave(job, Job.Status.CANCELLING, null);
                killOngoingSteps(job, job.isOnPostSteps());
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                sm.execute(context.getCurrent(), Cancelled, context);
            }
        });

        sm.add(RunningToCancelled, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) throws ScriptException {
                Job job = context.getJob();

                List<Step> steps = stepService.list(job, Sets.newHashSet(WAITING_AGENT));
                stepService.toStatus(steps, Step.Status.SKIPPED, null);

                JobAgent jobAgent = getJobAgent(job.getId());
                steps = stepService.list(job, Sets.newHashSet(RUNNING));

                // no busy agents, run post steps directly if needed
                if (getBusyAgents(jobAgent, steps).isEmpty()) {
                    runPostStepsIfNeeded(context);
                    return;
                }

                sm.execute(context.getCurrent(), Cancelling, context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.getJob();
                killOngoingSteps(job, job.isOnPostSteps());
                setJobStatusAndSave(job, Job.Status.CANCELLED, e.getMessage());
            }
        });
    }

    private void fromCancelling() {
        sm.add(CancellingToCancelled, new JobActionBase() {
            @Override
            public void accept(JobSmContext context) throws ScriptException {
                Job job = context.getJob();
                if (!runPostStepsIfNeeded(context)) {
                    setJobStatusAndSave(job, Job.Status.CANCELLED, null);
                }
            }
        });
    }

    /**
     * Release agent from job, and try to assign to waiting step, or release to pool
     *
     * @return true = agent assigned to other waiting step, false = released
     */
    private boolean releaseAgentOrAssignToWaitingStep(Job job, Step step) {
        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(step.getNodePath());

        if (node.isLastChildOfParent()) {
            String agentId = releaseAgentFromJob(job, node, step);
            if (assignAgentToWaitingStep(agentId, job, tree, false)) {
                return true;
            }
            releaseAgentToPool(job, node, step);
        }
        return false;
    }

    private boolean waitIfJobNotOnTopPriority(JobSmContext context) {
        Job job = context.getJob();

        while (true) {
            if (job.isExpired()) {
                context.setError(new Exception("time out while queueing"));
                sm.execute(context.getCurrent(), Timeout, context);
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
            job = getJob(job.getId());
            log.debug("Job {}/{} wait since not on top priority", job.getFlowName(), job.getBuildNumber());
        }
    }

    /**
     * All busy agents, which are occupied by flow and assigned to step
     */
    private Collection<Agent> getBusyAgents(JobAgent jobAgent, Collection<Step> ongoingSteps) {
        Map<String, Set<String>> agents = jobAgent.getAgents();
        Set<Agent> busy = new HashSet<>(agents.size());

        agents.forEach((agentId, v) -> {
            if (v.isEmpty()) {
                return;
            }

            Agent agent = agentService.get(agentId);
            if (!agent.isBusy()) {
                return;
            }

            for (Step s : ongoingSteps) {
                if (s.hasAgent() && s.getAgentId().equals(agentId)) {
                    busy.add(agent);
                    return;
                }
            }
        });
        return busy;
    }

    private Optional<Agent> fetchAgentFromJob(Job job, Node node) {
        FlowNode flow = node.getParent(FlowNode.class);
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
        FlowNode flow = node.getParent(FlowNode.class);
        Selector selector = flow.fetchSelector();

        // find agent outside job, blocking thread
        Optional<Agent> optional = agentService.acquire(job.getId(), selector);
        if (optional.isPresent()) {
            Agent agent = optional.get();
            AgentProfile profile = agentService.getProfile(agent.getToken());
            job.addAgentSnapshot(agent, profile);
            jobAgentDao.addFlowToAgent(job.getId(), agent.getId(), flow.getPathAsString());
            setJobStatusAndSave(job, job.getStatus(), null);
            return optional;
        }

        return Optional.empty();
    }

    private boolean assignAgentToWaitingStep(String agentId, Job job, NodeTree tree, boolean shouldIdle) {
        List<Step> steps = stepService.list(job, Lists.newArrayList(WAITING_AGENT));
        if (steps.isEmpty()) {
            return false;
        }

        for (Step waitingForAgentStep : steps) {
            if (!waitingForAgentStep.isStepType()) {
                continue;
            }

            Node n = tree.get(waitingForAgentStep.getNodePath());
            FlowNode f = n.getParent(FlowNode.class);
            Selector s = f.fetchSelector();

            Optional<Agent> acquired = agentService.acquire(job.getId(), s, agentId, shouldIdle);
            if (acquired.isPresent()) {
                Agent agent = acquired.get();
                AgentProfile profile = agentService.getProfile(agent.getToken());
                job.addAgentSnapshot(agent, profile);
                setJobStatusAndSave(job, job.getStatus(), null);

                jobAgentDao.addFlowToAgent(job.getId(), agent.getId(), f.getPathAsString());
                dispatch(job, n, waitingForAgentStep, agent);
                return true;
            }
        }

        return false;
    }

    private String releaseAgentFromJob(Job job, Node node, Step step) {
        String agentId = step.getAgentId();
        FlowNode flow = node.getParent(FlowNode.class);
        jobAgentDao.removeFlowFromAgent(job.getId(), agentId, flow.getPathAsString());
        return agentId;
    }

    private void releaseAgentToPool(Job job, Node node, Step step) {
        JobAgent jobAgent = getJobAgent(job.getId());
        String agentId = step.getAgentId();

        if (jobAgent.isOccupiedByFlow(agentId)) {
            return;
        }

        NodeTree tree = ymlManager.getTree(job);
        Selector currentSelector = node.getParent(FlowNode.class).fetchSelector();

        // find selectors of pending steps
        List<Step> notStartedSteps = stepService.list(job, Executed.WaitingStatus);
        Set<Selector> selectors = new HashSet<>(tree.getSelectors().size());
        for (Step s : notStartedSteps) {
            Node n = tree.get(s.getNodePath());
            Selector selector = n.getParent(FlowNode.class).getSelector();
            selectors.add(selector);
        }

        // keep agent for job
        if (selectors.contains(currentSelector)) {
            return;
        }

        agentService.release(Sets.newHashSet(agentId));
        jobAgentDao.removeAgent(job.getId(), agentId);
    }

    private void toFailureStatus(Job job, CIException e) {
        JobSmContext context = new JobSmContext(job.getId());
        context.setJob(job);
        context.setCurrent(new Status(job.getStatus().name()));
        context.setTo(Failure);
        context.setError(e);
        sm.execute(context);
    }

    private void setupJobYamlAndSteps(Job job, JobYml yml) {
        ymlManager.create(yml);
        stepService.init(job);

        FlowNode root = ymlManager.parse(yml);
        job.getContext().merge(root.getEnvironments(), false);
    }

    private void killOngoingSteps(Job job, boolean includePost) {
        List<Step> steps = stepService.list(job, Sets.newHashSet(WAITING_AGENT));
        stepService.toStatus(steps, Step.Status.SKIPPED, null);

        steps = stepService.list(job, Sets.newHashSet(RUNNING));
        Iterator<Step> iter = steps.iterator();

        while (iter.hasNext()) {
            Step step = iter.next();

            if (!includePost && step.isPost()) {
                iter.remove();
                continue;
            }

            if (step.hasAgent()) {
                Agent agent = agentService.get(step.getAgentId());
                if (agent.isBusy()) {
                    CmdIn killCmd = cmdManager.createKillCmd();
                    agentService.dispatch(killCmd, agent);
                    iter.remove(); // update step status from callback
                }
            }
        }

        stepService.toStatus(steps, Step.Status.KILLING, null);
    }

    private void onTransition(String jobId, Status to, Consumer<JobSmContext> onContext) {
        Optional<InterLock> lock = lockManager.lock(jobId);
        if (lock.isEmpty()) {
            Job job = getJob(jobId);
            toFailureStatus(job, new CIException("Fail to lock job"));
            return;
        }

        log.debug("Job {} is locked", jobId);
        Job job = getJob(jobId);

        JobSmContext context = new JobSmContext(jobId);
        context.setLock(lock.get());
        context.setJob(job);
        context.setCurrent(new Status(job.getStatus().name()));
        context.setTo(to);

        if (onContext != null) {
            onContext.accept(context);
        }

        sm.execute(context);
    }

    private JobYml fetchYamlFromGit(Job job) {
        final String gitUrl = JobContextHelper.getGitUrl(job);

        if (!StringHelper.hasValue(gitUrl)) {
            throw new NotAvailableException("Git url is missing");
        }

        final Path dir = getFlowRepoDir(gitUrl, job.getYamlRepoBranch());

        try {
            var secret = JobContextHelper.getSecretName(job);
            GitClient client = new GitClient(gitUrl, tmpDir, getSimpleSecret(secret));
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

            var jobYml = new JobYml(job.getId());
            jobYml.add(FlowYml.DEFAULT_NAME, StringHelper.toBase64(new String(ymlInBytes)));

            return jobYml;
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
     * @param job  current job
     * @param step current step
     * @return true if next step dispatched or have to wait for previous steps, false if no more steps or failure
     */
    private boolean toNextStep(Job job, Step step) throws ScriptException {
        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(NodePath.create(step.getNodePath())); // current node

        List<Node> next = node.getNext();
        if (job.isOnPostSteps()) {
            next = tree.post(step.getNodePath());
        }

        if (next.isEmpty()) {
            Collection<Node> ends = Sets.newHashSet(tree.ends());
            ends.remove(node); // do not check current node

            Set<Executed.Status> status = getStepsStatus(job, ends);
            return !Collections.disjoint(status, Executed.OngoingStatus);
        }

        // check prev steps status
        Collection<Node> prevs = tree.prevs(next, job.isOnPostSteps());
        Set<Executed.Status> previous = getStepsStatus(job, prevs);
        boolean hasFailure = !Collections.disjoint(previous, Executed.FailureStatus);
        if (hasFailure) {
            return false;
        }

        boolean hasOngoing = !Collections.disjoint(previous, Executed.OngoingStatus);
        boolean hasWaiting = !Collections.disjoint(previous, Executed.WaitingStatus);

        // do not execute next
        if (hasOngoing || hasWaiting) {
            return true;
        }

        executeJob(job, next);
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

    private void executeJob(Job job, List<Node> nodes) throws ScriptException {
        setJobStatusAndSave(job, job.getStatus(), null);

        NodeTree tree = ymlManager.getTree(job);

        for (Node node : nodes) {
            boolean condition = runCondition(job, node);
            Step step = stepService.get(job.getId(), node.getPathAsString());

            if (!condition) {
                setSkipStatusToStep(step);
                updateJobContextAndLatestStatus(job, step);

                List<Node> next = tree.skip(node.getPath());
                executeJob(job, next);
                continue;
            }

            // skip current node cmd dispatch if the node has children
            if (node.hasChildren()) {
                executeJob(job, node.getNext());
                continue;
            }

            // add dispatchable step
            job.addToCurrentPath(step);
            jobDao.save(job);

            Optional<Agent> optionalFromJob = fetchAgentFromJob(job, node);
            if (optionalFromJob.isPresent()) {
                dispatch(job, node, step, optionalFromJob.get());
                continue;
            }

            Optional<Agent> optionalFromPool = fetchAgentFromPool(job, node);
            if (optionalFromPool.isPresent()) {
                Agent agent = optionalFromPool.get();
                dispatch(job, node, step, agent);
                continue;
            }

            stepService.toStatus(step, WAITING_AGENT, null, false);
        }
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
        stepService.toStatus(step, RUNNING, null, false);

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
        Job job = context.getJob();

        Job.Status statusFromContext = JobContextHelper.getStatus(job);
        String error = JobContextHelper.getError(job);
        ObjectsHelper.ifNotNull(error, s -> context.setError(new CIException(s)));

        sm.execute(context.getCurrent(), new Status(statusFromContext.name()), context);
    }

    /**
     * To running post status, skip current state machine action and switch job status to Running
     * Return `true` if post step found
     */
    private boolean runPostStepsIfNeeded(JobSmContext context) throws ScriptException {
        if (context.getError() == Errors.AgentOffline) {
            return false;
        }

        Job job = context.getJob();
        Step step = context.getStep();
        Objects.requireNonNull(step, "The step not defined when running post steps");

        if (job.isOnPostSteps()) {
            return true;
        }

        NodeTree tree = ymlManager.getTree(job);
        List<Node> nextPostSteps = tree.post(step.getNodePath());
        if (nextPostSteps.isEmpty()) {
            return false;
        }

        // remove running or finished post steps
        Iterator<Node> iterator = nextPostSteps.iterator();
        while (iterator.hasNext()) {
            Node postNode = iterator.next();
            Step postStep = stepService.get(job.getId(), postNode.getPathAsString());
            if (postStep.isOngoing() || postStep.isKilling() || postStep.isFinished()) {
                iterator.remove();
            }
        }

        // check current all steps that should be in finish status
        List<Step> steps = stepService.listByPath(job, job.getCurrentPath());
        for (Step s : steps) {
            log.debug("Step {} status ------------- {}", s.getNodePath(), s.getStatus());

            if (s.isOngoing() || s.isKilling()) {
                context.setSkip(true);
                log.debug("Step ({} = {}) is ongoing or killing status", s.getNodePath(), s.getStatus());
                return true;
            }
        }

        log.debug("All current steps finished, will run post steps");
        context.setSkip(true);

        job.setOnPostSteps(true);
        job.resetCurrentPath();
        job.setStatus(Job.Status.RUNNING);
        JobContextHelper.setStatus(job, context.getTo().getName());

        log.debug("Run post steps: {}", nextPostSteps);
        log.debug("---- Job Status Before Post {} {}", job.getStatus(), JobContextHelper.getStatus(job));
        executeJob(job, nextPostSteps);
        return true;
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
        JobContextHelper.setStatus(job, newStatus);

        jobDao.save(job);
        eventManager.publish(new JobStatusChangeEvent(this, job));
        logInfo(job, "status = {}", job.getStatus());
    }

    private void updateJobContextAndLatestStatus(Job job, Step step) {
        job.setFinishAt(step.getFinishAt());

        // remove step path if success, keep failure for rerun later
        if (step.isSuccess()) {
            job.removeFromCurrentPath(step);
        }

        // merge output to job context
        Vars<String> context = job.getContext();
        context.merge(step.getOutput());

        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());
        context.put(Variables.Job.DurationInSeconds, String.valueOf(job.getDurationInSeconds()));
        context.put(Variables.Job.Steps, stepService.toVarString(job, step));

        // DO NOT update job status from post step
        if (step.isPost()) {
            return;
        }

        // DO NOT update job status from context
        JobContextHelper.setStatus(job, StatusHelper.convert(step));
        JobContextHelper.setError(job, step.getError());
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

    private Job getJob(String jobId) {
        return jobDao.findById(jobId).get();
    }

    private class ActionOnFinishStatus implements Consumer<JobSmContext> {

        @Override
        public void accept(JobSmContext context) {
            Job job = context.getJob();

            // save job with status
            Throwable error = context.getError();
            String message = error == null ? "" : error.getMessage();
            setJobStatusAndSave(job, context.getTargetToJobStatus(), message);
            jobPriorityDao.removeJob(job.getFlowId(), job.getBuildNumber());

            JobAgent agents = getJobAgent(job.getId());
            agentService.release(agents.all());

            eventManager.publish(new JobFinishedEvent(this, job));
        }
    }

    private abstract class JobActionBase extends Action<JobSmContext> {

        @Override
        public void onFinally(JobSmContext context) {
            InterLock lock = context.getLock();
            if (lock == null) {
                return;
            }

            Job job = context.getJob();
            lockManager.unlock(lock, job.getId());
            context.setLock(null);
        }
    }
}
