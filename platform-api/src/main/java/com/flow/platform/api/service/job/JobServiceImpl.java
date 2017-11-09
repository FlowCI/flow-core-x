/*
 * Copyright 2017 flow.ci
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
package com.flow.platform.api.service.job;

import static com.flow.platform.api.domain.job.NodeStatus.FAILURE;
import static com.flow.platform.api.domain.job.NodeStatus.STOPPED;
import static com.flow.platform.api.domain.job.NodeStatus.SUCCESS;
import static com.flow.platform.api.domain.job.NodeStatus.TIMEOUT;
import static com.flow.platform.api.envs.FlowEnvs.FLOW_STATUS;
import static com.flow.platform.api.envs.FlowEnvs.FLOW_YML_STATUS;
import static com.flow.platform.api.envs.FlowEnvs.StatusValue;

import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.dao.job.JobYmlDao;
import com.flow.platform.api.dao.job.NodeResultDao;
import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.envs.JobEnvs;
import com.flow.platform.api.events.JobStatusChangeEvent;
import com.flow.platform.api.git.GitEventEnvConverter;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.EnvService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.http.HttpURL;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yh@firim
 */
@Service
public class JobServiceImpl extends ApplicationEventService implements JobService {

    private static Logger LOGGER = new Logger(JobService.class);

    private final Integer createSessionRetryTimes = 5;

    @Value("${task.job.toggle.execution_timeout}")
    private Boolean isJobTimeoutExecuteTimeout;

    @Value("${task.job.toggle.execution_create_session_duration}")
    private Long jobExecuteTimeoutCreateSessionDuration;

    @Value("${task.job.toggle.execution_running_duration}")
    private Long jobExecuteTimeoutRunningDuration;

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private PlatformQueue<CmdCallbackQueueItem> cmdCallbackQueue;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private GitService gitService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private EnvService envService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private JobYmlDao jobYmlDao;

    @Value(value = "${domain.api}")
    private String apiDomain;

    @Override
    public Job find(String flowName, Integer number) {
        Job job = jobDao.get(flowName, number);
        return find(job);
    }

    @Override
    public Job find(BigInteger jobId) {
        Job job = jobDao.get(jobId);
        return find(job);
    }

    @Override
    public Job find(String sessionId) {
        return jobDao.get(sessionId);
    }

    @Override
    public String findYml(String path, Integer number) {
        Job job = find(path, number);
        return jobNodeService.find(job).getFile();
    }

    @Override
    public List<Job> list(List<String> paths, boolean latestOnly) {
        if (latestOnly) {
            return jobDao.latestByPath(paths);
        }
        return jobDao.listByPath(paths);
    }

    @Override
    @Transactional(noRollbackFor = FlowException.class)
    public Job createFromFlowYml(String path, JobCategory eventType, Map<String, String> envs, User creator) {
        // verify flow yml status
        Node flow = nodeService.find(path).root();
        String ymlStatus = flow.getEnv(FlowEnvs.FLOW_YML_STATUS);

        if (!Objects.equals(ymlStatus, YmlStatusValue.FOUND.value())) {
            throw new IllegalStatusException("Illegal yml status for flow " + flow.getName());
        }

        // get yml content
        Yml yml = ymlService.get(flow);

        // create job instance
        Job job = createJob(path, eventType, envs, creator);
        new OnYmlSuccess(job, null).accept(yml);
        return job;
    }

    @Override
    @Transactional(noRollbackFor = FlowException.class)
    public void createWithYmlLoad(String path,
                                  JobCategory eventType,
                                  Map<String, String> envs,
                                  User creator,
                                  Consumer<Job> onJobCreated) {

        // find flow and reset yml status
        Node flow = nodeService.find(path).root();
        envService.save(flow, EnvUtil.build(FLOW_YML_STATUS, YmlStatusValue.NOT_FOUND), false);

        // merge input env to flow for git loading, not save to flow since the envs is for job
        EnvUtil.merge(envs, flow.getEnvs(), true);

        // create job
        Job job = createJob(path, eventType, envs, creator);
        updateJobStatusAndSave(job, JobStatus.YML_LOADING);

        // load yml
        ymlService.startLoad(flow, new OnYmlSuccess(job, onJobCreated), new OnYmlError(job));
    }

    @Override
    public void callback(CmdCallbackQueueItem cmdQueueItem) {
        BigInteger jobId = cmdQueueItem.getJobId();
        Cmd cmd = cmdQueueItem.getCmd();
        Job job = jobDao.get(jobId);

        // if not found job, re enqueue
        if (job == null) {
            throw new NotFoundException("job");
        }

        if (Job.FINISH_STATUS.contains(job.getStatus())) {
            LOGGER.trace("Reject cmd callback since job %s already in finish status", job.getId());
            return;
        }

        if (cmd.getType() == CmdType.CREATE_SESSION) {
            onCreateSessionCallback(job, cmd);
            return;
        }

        if (cmd.getType() == CmdType.RUN_SHELL) {
            String path = cmd.getExtra();
            if (Strings.isNullOrEmpty(path)) {
                throw new IllegalParameterException("Node path is required for cmd RUN_SHELL callback");
            }

            onRunShellCallback(path, cmd, job);
            return;
        }

        if (cmd.getType() == CmdType.DELETE_SESSION) {
            LOGGER.trace("Session been deleted for job: %s", cmdQueueItem.getJobId());
            return;
        }

        LOGGER.warn("not found cmdType, cmdType: %s", cmd.getType().toString());
        throw new NotFoundException("not found cmdType");
    }

    @Override
    @Transactional
    public void delete(String path) {
        List<BigInteger> jobIds = jobDao.findJobIdsByPath(path);
        // TODO :  Late optimization and paging jobIds
        if (jobIds.size() > 0) {
            //first clear agent jobs
            stopAllJobs(path);

            jobYmlDao.delete(jobIds);
            nodeResultDao.delete(jobIds);
            jobDao.deleteJob(path);
        }
    }

    private void stopAllJobs(String path) {
        LOGGER.trace("before delete flow, first stop all jobs");
        List<Job> jobs = jobDao.listByPath(Arrays.asList(path));
        List<Job> sessionCreateJobs = new LinkedList<>();
        List<Job> runningJobs = new LinkedList<>();

        for (Job job : jobs) {
            if (job.getStatus() == JobStatus.SESSION_CREATING) {
                sessionCreateJobs.add(job);
            }

            if (job.getStatus() == JobStatus.RUNNING) {
                runningJobs.add(job);
            }
        }

        // first to stop session create job
        for (Job sessionCreateJob : sessionCreateJobs) {
            stop(path, sessionCreateJob.getNumber());
        }

        // last to stop running job
        for (Job runningJob : runningJobs) {
            stop(path, runningJob.getNumber());
        }
        LOGGER.trace("before delete flow, finish stop all jobs");
    }


    private Job createJob(String path, JobCategory eventType, Map<String, String> envs, User creator) {
        Node root = nodeService.find(PathUtil.rootPath(path)).root();
        if (root == null) {
            throw new IllegalParameterException("Path does not existed");
        }

        if (creator == null) {
            throw new IllegalParameterException("User is required while create job");
        }

        // verify required envs for create job
        if (!EnvUtil.hasRequiredEnvKey(root, REQUIRED_ENVS)) {
            throw new IllegalStatusException("Missing required env vailable for flow " + path);
        }

        // verify flow status
        String status = root.getEnv(FLOW_STATUS);
        if (!Objects.equals(status, StatusValue.READY.value())) {
            throw new IllegalStatusException("Cannot create job since status is not READY");
        }

        // create job
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath(root.getPath());
        job.setNodeName(root.getName());
        job.setNumber(jobDao.maxBuildNumber(job.getNodePath()) + 1);
        job.setCategory(eventType);
        job.setCreatedBy(creator.getEmail());
        job.setCreatedAt(ZonedDateTime.now());
        job.setUpdatedAt(ZonedDateTime.now());

        // setup job env variables
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_CATEGORY, eventType.name());
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_NUMBER, job.getNumber().toString());
        job.putEnv(JobEnvs.FLOW_JOB_LOG_PATH, logUrl(job));

        EnvUtil.merge(root.getEnvs(), job.getEnvs(), true);
        EnvUtil.merge(envs, job.getEnvs(), true);

        //save job
        return jobDao.save(job);
    }

    /**
     * run node
     *
     * @param node job node's script and record cmdId and sync send http
     */
    private void run(Node node, Job job) {
        if (node == null) {
            throw new IllegalParameterException("Cannot run node with null value");
        }

        NodeTree tree = jobNodeService.get(job);

        if (!tree.canRun(node.getPath())) {
            // run next node
            Node next = tree.next(node.getPath());
            run(next, job);
            return;
        }

        // pass job env to node
        EnvUtil.merge(job.getEnvs(), node.getEnvs(), false);

        // pass root node output to current node
        NodeResult rootResult = nodeResultService.find(tree.root().getPath(), job.getId());
        EnvUtil.merge(rootResult.getOutputs(), node.getEnvs(), false);

        // to run node with customized cmd id
        try {
            NodeResult nodeResult = nodeResultService.find(node.getPath(), job.getId());
            CmdInfo cmd = cmdService.runShell(job, node, nodeResult.getCmdId());
        } catch (IllegalStatusException e) {
            CmdInfo rawCmd = (CmdInfo) e.getData();
            rawCmd.setStatus(CmdStatus.EXCEPTION);
            nodeResultService.updateStatusByCmd(job, node, Cmd.convert(rawCmd), e.getMessage());
        }
    }

    /**
     * Create session callback
     */
    private void onCreateSessionCallback(Job job, Cmd cmd) {
        if (cmd.getStatus() != CmdStatus.SENT) {

            if (cmd.getRetry() > 1) {
                LOGGER.trace("Create session failure but retrying: %s", cmd.getStatus().getName());
                return;
            }

            final String errMsg = "Create session failure with cmd status: " + cmd.getStatus().getName();
            LOGGER.warn(errMsg);

            job.setFailureMessage(errMsg);
            updateJobStatusAndSave(job, JobStatus.FAILURE);
            return;
        }

        // run step
        NodeTree tree = jobNodeService.get(job);
        if (tree == null) {
            throw new NotFoundException("Cannot fond related node tree for job: " + job.getId());
        }

        // set job properties
        job.setSessionId(cmd.getSessionId());
        job.putEnv(JobEnvs.FLOW_JOB_AGENT_INFO, cmd.getAgentPath().toString());
        updateJobStatusAndSave(job, JobStatus.RUNNING);

        // start run flow from fist node
        run(tree.first(), job);
    }

    /**
     * Run shell callback
     */
    private void onRunShellCallback(String path, Cmd cmd, Job job) {
        NodeTree tree = jobNodeService.get(job);
        Node node = tree.find(path);
        Node next = tree.next(path);

        // bottom up recursive update node result
        NodeResult nodeResult = nodeResultService.updateStatusByCmd(job, node, cmd, null);
        LOGGER.debug("Run shell callback for node result: %s", nodeResult);

        // no more node to run and status is not running
        if (next == null && !nodeResult.isRunning()) {
            stopJob(job);
            return;
        }

        // continue to run if on success status
        if (nodeResult.isSuccess()) {
            run(next, job);
            return;
        }

        // continue to run if allow failure on failure status
        if (nodeResult.isFailure() && nodeResult.getNodeTag() == NodeTag.STEP) {
            Node step = node;
            if (step.getAllowFailure()) {
                run(next, job);
            }

            // clean up session if node result failure and set job status to error

            //TODO: Missing unit test
            else {
                stopJob(job);
            }
        }
    }

    @Override
    public void enterQueue(CmdCallbackQueueItem cmdQueueItem) {
        cmdCallbackQueue.enqueue(cmdQueueItem);
    }

    @Override
    public Job stop(String path, Integer buildNumber) {
        Job runningJob = find(path, buildNumber);
        NodeResult result = runningJob.getRootResult();

        if (result == null) {
            throw new NotFoundException("running job not found node result - " + path);
        }

        if (!result.isRunning()) {
            return runningJob;
        }

        // do not handle job since it is not in running status
        try {
            final HashSet<NodeStatus> skipStatus = Sets.newHashSet(SUCCESS, FAILURE, TIMEOUT);
            nodeResultService.updateStatus(runningJob, STOPPED, skipStatus);

            stopJob(runningJob);
        } catch (Throwable throwable) {
            String message = "stop job error - " + ExceptionUtil.findRootCause(throwable);
            LOGGER.traceMarker("stopJob", message);
            throw new IllegalParameterException(message);
        }

        return runningJob;
    }

    @Override
    public Job update(Job job) {
        jobDao.update(job);
        return job;
    }

    /**
     * Update job instance with new job status and boardcast JobStatusChangeEvent
     */
    @Override
    public void updateJobStatusAndSave(Job job, JobStatus newStatus) {
        JobStatus originStatus = job.getStatus();

        if (originStatus == newStatus) {
            jobDao.update(job);
            return;
        }

        //if job has finish not to update status
        if (Job.FINISH_STATUS.contains(originStatus)) {
            return;
        }
        job.setStatus(newStatus);
        jobDao.update(job);

        this.dispatchEvent(new JobStatusChangeEvent(this, job, originStatus, newStatus));
    }

    @Transactional(noRollbackFor = FlowException.class)
    public void createJobNodesAndCreateSession(Job job, String yml) {
        //create yml snapshot for job
        jobNodeService.save(job, yml);

        // set root node env from yml to job env
        Node root = jobNodeService.get(job).root();
        EnvUtil.merge(root.getEnvs(), job.getEnvs(), true);

        // init for node result and set to job object
        List<NodeResult> resultList = nodeResultService.create(job);
        NodeResult rootResult = resultList.remove(resultList.size() - 1);
        job.setRootResult(rootResult);
        job.setChildrenResult(resultList);

        // to create agent session for job
        try {
            String sessionId = cmdService.createSession(job, createSessionRetryTimes);
            job.setSessionId(sessionId);
            updateJobStatusAndSave(job, JobStatus.SESSION_CREATING);
        } catch (IllegalStatusException e) {
            job.setFailureMessage(e.getMessage());
            updateJobStatusAndSave(job, JobStatus.FAILURE);
        }
    }

    private class OnYmlSuccess implements Consumer<Yml> {

        private final Job job;

        private final String path;

        private final Consumer<Job> onJobCreated;

        public OnYmlSuccess(Job job, Consumer<Job> onJobCreated) {
            this.job = job;
            this.path = job.getNodePath();
            this.onJobCreated = onJobCreated;
        }

        @Override
        public void accept(Yml yml) {
            LOGGER.trace("Yml content has been loaded for path : " + path);
            Node root = nodeService.find(PathUtil.rootPath(path)).root();

            // set git commit info to job env
            if (job.getCategory() == JobCategory.MANUAL
                || job.getCategory() == JobCategory.SCHEDULER
                || job.getCategory() == JobCategory.API) {

                try {
                    GitCommit gitCommit = gitService.latestCommit(root);
                    Map<String, String> envFromCommit = GitEventEnvConverter.convert(gitCommit);
                    EnvUtil.merge(envFromCommit, job.getEnvs(), true);
                    jobDao.update(job);
                } catch (IllegalStatusException exceptionFromGit) {
                    LOGGER.warn(exceptionFromGit.getMessage());
                }
            }

            createJobNodesAndCreateSession(job, yml.getFile());

            try {
                if (onJobCreated != null) {
                    onJobCreated.accept(job);
                }
            } catch (Throwable e) {
                LOGGER.warn("Fail to create job for path %s : %s ", path, ExceptionUtil.findRootCause(e).getMessage());
            }
        }
    }

    /**
     *
     */
    private class OnYmlError implements Consumer<Throwable> {

        private final Job job;

        public OnYmlError(Job job) {
            this.job = job;
        }

        @Override
        public void accept(Throwable throwable) {
            job.setFailureMessage(throwable.getMessage());
            updateJobStatusAndSave(job, JobStatus.FAILURE);
        }
    }

    /**
     * Update job status by root node result
     */
    private NodeResult setJobStatusByRootResult(Job job) {
        NodeResult rootResult = nodeResultService.find(job.getNodePath(), job.getId());
        JobStatus newStatus = job.getStatus();

        if (rootResult.isFailure()) {
            newStatus = JobStatus.FAILURE;
        }

        if (rootResult.isSuccess()) {
            newStatus = JobStatus.SUCCESS;
        }

        if (rootResult.isStop()) {
            newStatus = JobStatus.STOPPED;
        }

        updateJobStatusAndSave(job, newStatus);
        return rootResult;
    }

    private Job find(Job job) {
        if (job == null) {
            throw new NotFoundException("Job is not found");
        }

        List<NodeResult> childrenResult = nodeResultService.list(job, true);
        job.setChildrenResult(childrenResult);
        return job;
    }

    /**
     * Update job status and delete agent session
     */
    private void stopJob(Job job) {
        setJobStatusByRootResult(job);
        cmdService.deleteSession(job);
    }

    @Override
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
    public void checkTimeoutTask() {
        if (!isJobTimeoutExecuteTimeout) {
            return;
        }

        LOGGER.trace("job timeout task start");

        // create session job timeout 6s time out
        ZonedDateTime finishZoneDateTime = ZonedDateTime.now().minusSeconds(jobExecuteTimeoutCreateSessionDuration);
        List<Job> jobs = jobDao.listForExpired(finishZoneDateTime, JobStatus.SESSION_CREATING);
        for (Job job : jobs) {
            updateJobAndNodeResultTimeout(job);
        }

        // running job timeout 1h time out
        ZonedDateTime finishRunningZoneDateTime = ZonedDateTime.now().minusSeconds(jobExecuteTimeoutRunningDuration);
        List<Job> runningJobs = jobDao.listForExpired(finishRunningZoneDateTime, JobStatus.RUNNING);
        for (Job job : runningJobs) {
            updateJobAndNodeResultTimeout(job);
        }

        LOGGER.trace("job timeout task end");
    }

    private void updateJobAndNodeResultTimeout(Job job) {
        // if job is running , please delete session first
        if (job.getStatus() == JobStatus.RUNNING) {
            try {
                cmdService.deleteSession(job);
            } catch (Throwable e) {
                LOGGER.warn(
                    "Error on delete session for job %s: %s",
                    job.getId(),
                    ExceptionUtil.findRootCause(e).getMessage());
            }
        }

        updateJobStatusAndSave(job, JobStatus.TIMEOUT);
        nodeResultService.updateStatus(job, NodeStatus.TIMEOUT, NodeResult.FINISH_STATUS);
    }

    private String logUrl(final Job job) {
        return HttpURL.build(apiDomain)
            .append("/jobs/")
            .append(job.getNodeName())
            .append(job.getNumber().toString())
            .append("/log/download").toString();
    }
}
