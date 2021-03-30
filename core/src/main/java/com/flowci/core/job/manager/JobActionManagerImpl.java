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

package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentProfile;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.event.IdleAgentEvent;
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
import java.util.function.Consumer;

import static com.flowci.core.job.domain.Executed.Status.RUNNING;
import static com.flowci.core.job.domain.Executed.Status.WAITING_AGENT;

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
    private static final Status RunningPost = new Status(Job.Status.RUNNING_POST.name());
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
    private static final Transition RunningToRunningPost = new Transition(Running, RunningPost);

    // running post
    private static final Transition RunningPostToRunningPost = new Transition(RunningPost, RunningPost);
    private static final Transition RunningPostToFailure = new Transition(RunningPost, Failure);
    private static final Transition RunningPostToCancelling = new Transition(RunningPost, Cancelling);
    private static final Transition RunningPostToCanceled = new Transition(RunningPost, Cancelled);
    private static final Transition RunningPostToTimeout = new Transition(RunningPost, Timeout);

    // cancelling
    private static final Transition CancellingToCancelled = new Transition(Cancelling, Cancelled);
    private static final Transition CancellingToRunningPost = new Transition(Cancelling, RunningPost);

    private static final long RetryInterval = 10 * 1000; // 10 seconds

    private static final int DefaultJobLockTimeout = 20; // seconds

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
            fromRunningPost();
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

            Optional<InterLock> lock = lock(job.getId(), "LockJobFromIdleAgent");
            if (!lock.isPresent()) {
                toFailureStatus(job, null, new CIException("Fail to lock job"));
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
                toFailureStatus(job, null, new CIException(e.getMessage()));
            } finally {
                unlock(lock.get(), "LockJobFromIdleAgent");
            }
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

        if (job.isRunningPost()) {
            on(job, Job.Status.RUNNING_POST, (context) -> context.step = step);
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
                doFromXToCreated(context);
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
                doFromXToCreated(context);
            }
        });
    }

    private void doFromXToCreated(JobSmContext context) {
        Job job = context.job;
        String yml = context.yml;

        setupJobYamlAndSteps(job, yml);
        setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
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
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

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

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
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

                updateJobContextAndLatestStatus(job, step);
                setJobStatusAndSave(job, Job.Status.RUNNING, null);
                log.debug("Step {} been recorded", step.getNodePath());

                if (!step.isSuccess()) {
                    toFinishStatus(context);
                    return;
                }

                if (releaseAgentOrAssignToWaitingStep(job, step)) {
                    return;
                }

                if (toNextStep(job, step, false)) {
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
            public void accept(JobSmContext context) {
                Job job = context.job;
                killOngoingSteps(job);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
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
                killOngoingSteps(job);

                toRunningPostStatusIfNeeded(context);
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
                killOngoingSteps(job);
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
                List<Step> ongoingSteps = stepService.list(job, Executed.OngoingStatus);

                // no busy agents, run post steps directly if needed
                if (jobAgent.allBusyAgents(ongoingSteps).isEmpty()) {
                    toRunningPostStatusIfNeeded(context);
                    return;
                }

                sm.execute(context.getCurrent(), Cancelling, context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                killOngoingSteps(job);
                setJobStatusAndSave(job, Job.Status.CANCELLED, e.getMessage());
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });

        // from RunningToFailure or RunningToCancelled
        sm.add(RunningToRunningPost, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                doFromCancellingOrRunningToRunningPost(context);
            }

            @Override
            public void onException(Throwable e, JobSmContext context) {
                Job job = context.job;
                setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
            }
        });
    }

    private void fromRunningPost() {
        sm.add(RunningPostToRunningPost, new Action<JobSmContext>() {
            @Override
            public boolean canRun(JobSmContext context) {
                return lockJobBefore(context);
            }

            @Override
            public void accept(JobSmContext context) throws Exception {
                Job job = context.job;
                Step step = context.step;

                updateJobContextAndLatestStatus(job, step);
                setJobStatusAndSave(job, Job.Status.RUNNING_POST, null);
                log.debug("Step {} been recorded", step.getNodePath());

                if (releaseAgentOrAssignToWaitingStep(job, step)) {
                    return;
                }

                if (toNextStep(job, step, true)) {
                    return;
                }

                toFinishStatus(context);
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });

        Action<JobSmContext> toActualStatusAction = new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                logInfo(context.job, "RunningPost to {}", context.getTargetToJobStatus());
            }
        };

        sm.add(RunningPostToFailure, toActualStatusAction);
        sm.add(RunningPostToTimeout, toActualStatusAction);
        sm.add(RunningPostToCanceled, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                // TODO: send kill to all running steps
            }
        });

        sm.add(RunningPostToCancelling, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                sm.execute(context.getCurrent(), Cancelled, context);
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
                Step step = context.step;

                if (step == null) {
                    setJobStatusAndSave(job, Job.Status.CANCELLED, null);
                    return;
                }

                if (!toRunningPostStatusIfNeeded(context)) {
                    setJobStatusAndSave(job, Job.Status.CANCELLED, null);
                }
            }

            @Override
            public void onFinally(JobSmContext context) {
                unlockJobAfter(context);
            }
        });

        // from CancellingToCancelled
        sm.add(CancellingToRunningPost, new Action<JobSmContext>() {
            @Override
            public void accept(JobSmContext context) throws Exception {
                doFromCancellingOrRunningToRunningPost(context);
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

    private void doFromCancellingOrRunningToRunningPost(JobSmContext context) throws ScriptException {
        log.debug("---- CancellingOrRunningToRunningPost ----");

        Job job = context.job;
        Step step = context.step;

        NodeTree tree = ymlManager.getTree(job);
        List<Node> post = tree.post(step.getNodePath());

        // DO NOT check post size, it has been checked in previous
        job.setStatus(Job.Status.RUNNING_POST);
        executeJob(job, post);
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

        FlowNode root = ymlManager.parse(yml);
        job.getContext().merge(root.getEnvironments(), false);
    }

    private void killOngoingSteps(Job job) {
        List<Step> steps = stepService.list(job, Sets.newHashSet(WAITING_AGENT));
        stepService.toStatus(steps, Step.Status.SKIPPED, null);

        steps = stepService.list(job, Sets.newHashSet(RUNNING));
        Iterator<Step> iter = steps.iterator();

        while (iter.hasNext()) {
            Step step = iter.next();

            if (step.isPost()) {
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
    private boolean toNextStep(Job job, Step step, boolean post) throws ScriptException {
        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(NodePath.create(step.getNodePath())); // current node

        List<Node> next = node.getNext();
        if (post) {
            next = tree.post(step.getNodePath());
        }

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
        Job job = context.job;

        Job.Status statusFromContext = job.getStatusFromContext();
        String error = job.getErrorFromContext();
        ObjectsHelper.ifNotNull(error, s -> context.setError(new CIException(s)));

        sm.execute(context.getCurrent(), new Status(statusFromContext.name()), context);
    }

    /**
     * To running post status
     * Return `true` if post step found
     */
    private boolean toRunningPostStatusIfNeeded(JobSmContext context) {
        Job job = context.job;
        Step step = context.step;

        NodeTree tree = ymlManager.getTree(job);
        List<Node> post = tree.post(step.getNodePath());

        if (post.isEmpty()) {
            return false;
        }

        sm.execute(context.getCurrent(), RunningPost, context);
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
        job.setStatusToContext(newStatus);

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
        context.put(Variables.Job.Steps, stepService.toVarString(job, step));

        // DO NOT update job status from post step
        if (step.isPost()) {
            return;
        }

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

            JobAgent agents = getJobAgent(job.getId());
            agentService.release(agents.all());

            localTaskService.executeAsync(job);
        }
    }
}
