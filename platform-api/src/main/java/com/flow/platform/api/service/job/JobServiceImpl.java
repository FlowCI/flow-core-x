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

import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.domain.AgentWithFlow;
import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.JobEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.service.AgentService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class JobServiceImpl implements JobService {

    private static Logger LOGGER = new Logger(JobService.class);

    private Integer RETRY_TIMEs = 5;

    private final Integer createSessionRetryTimes = 5;

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private BlockingQueue<CmdCallbackQueueItem> cmdBaseBlockingQueue;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private AgentService agentService;

    @Override
    public Job find(String flowName, Integer number) {
        Job job = jobDao.get(flowName, number);
        if (job == null) {
            throw new NotFoundException("job is not found");
        }

        List<NodeResult> childrenResult = getChildrenResult(job);
        job.setChildrenResult(childrenResult);
        return job;
    }

    @Override
    public String findYml(String path, Integer number) {
        Job job = find(path, number);
        return jobNodeService.find(job.getId()).getFile();
    }

    @Override
    public List<NodeResult> listNodeResult(String path, Integer number) {
        Job job = find(path, number);
        return getChildrenResult(job);
    }

    @Override
    public List<Job> list(List<String> paths, boolean latestOnly) {
        if (latestOnly) {
            List<Job> jobs = jobDao.latestByPath(paths);
        }

        List<Job> jobs = jobDao.listByPath(paths);

        for (Job job : jobs) {
            job.setAgent(agentService.show(job));
        }

        return jobs;
    }

    @Override
    public Job createJob(String path) {
        Node root = nodeService.find(PathUtil.rootPath(path));
        if (root == null) {
            throw new IllegalParameterException("Path does not existed");
        }

        String status = root.getEnv(FlowEnvs.FLOW_STATUS);
        if (Strings.isNullOrEmpty(status) || !status.equals(FlowEnvs.StatusValue.READY.value())) {
            throw new IllegalStatusException("Cannot create job since status is not READY");
        }

        String yml = null;
        try {
            yml = ymlService.getYmlContent(root.getPath());
            if (Strings.isNullOrEmpty(yml)) {
                throw new IllegalStatusException("Yml is loading for path " + path);
            }
        } catch (FlowException e) {
            LOGGER.error("Fail to find yml content", e);
            throw e;
        }

        // create job
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath(root.getPath());
        job.setNodeName(root.getName());
        job.setNumber(jobDao.maxBuildNumber(job.getNodePath()) + 1);

        // setup job env variables
        job.putEnv(JobEnvs.JOB_BUILD_NUMBER, job.getNumber().toString());
        EnvUtil.merge(root.getEnvs(), job.getEnvs(), true);

        //save job
        jobDao.save(job);

        // create yml snapshot for job
        jobNodeService.save(job.getId(), yml);

        // init for node result and set to job object
        List<NodeResult> resultList = nodeResultService.create(job);
        NodeResult rootResult = resultList.remove(resultList.size() - 1);
        job.setRootResult(rootResult);
        job.setChildrenResult(resultList);

        // to create agent session for job
        String sessionId = cmdService.createSession(job, createSessionRetryTimes);
        job.setSessionId(sessionId);
        job.setStatus(JobStatus.SESSION_CREATING);
        jobDao.update(job);

        return job;
    }

    @Override
    public void callback(CmdCallbackQueueItem cmdQueueItem) {
        BigInteger jobId = cmdQueueItem.getJobId();
        Cmd cmd = cmdQueueItem.getCmd();
        Job job = jobDao.get(jobId);

        if (cmd.getType() == CmdType.CREATE_SESSION) {

            // TODO: refactor to find(id, timeout)
            if (job == null) {
                if (cmdQueueItem.getRetryTimes() < RETRY_TIMEs) {
                    try {
                        Thread.sleep(1000);
                        LOGGER.traceMarker("Callback", "Job not found, retry times - %s jobId - %s",
                            cmdQueueItem.getRetryTimes(), jobId);
                    } catch (Throwable throwable) {
                    }

                    cmdQueueItem.plus();
                    enterQueue(cmdQueueItem);
                    return;
                }

                LOGGER.warn("job not found, jobId: %s", jobId);
                throw new NotFoundException("job not found");
            }

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

    private List<NodeResult> getChildrenResult(Job job) {
        List<NodeResult> allResults = nodeResultService.list(job);
        allResults.remove(allResults.size() - 1);
        return allResults;
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

        NodeTree tree = jobNodeService.get(job.getId());

        if (!tree.canRun(node.getPath())) {
            // run next node
            Node next = tree.next(node.getPath());
            run(next, job);
            return;
        }

        // pass job env to node
        EnvUtil.merge(job.getEnvs(), node.getEnvs(), false);

        // to run node with customized cmd id
        NodeResult nodeResult = nodeResultService.find(node.getPath(), job.getId());
        CmdInfo cmd = cmdService.runShell(job, node, nodeResult.getCmdId());

        if (cmd.getStatus() == CmdStatus.EXCEPTION) {
            nodeResultService.update(job, node, Cmd.convert(cmd));
        }
    }

    /**
     * Create session callback
     */
    private void onCreateSessionCallback(Job job, Cmd cmd) {
        if (cmd.getStatus() != CmdStatus.SENT) {

            if (cmd.getRetry() > 1) {
                LOGGER.trace("Create Session fail but retrying: %s", cmd.getStatus().getName());
                return;
            }

            LOGGER.warn("Create Session Error Session Status - %s", cmd.getStatus().getName());
            job.setStatus(JobStatus.ERROR);
            jobDao.update(job);
            return;
        }

        // run step
        NodeTree tree = jobNodeService.get(job.getId());
        if (tree == null) {
            throw new NotFoundException("Cannot fond related node tree for job: " + job.getId());
        }

        // start run flow
        job.setStatus(JobStatus.RUNNING);
        job.setSessionId(cmd.getSessionId());
        jobDao.update(job);

        run(tree.first(), job);
    }

    /**
     * Run shell callback
     */
    private void onRunShellCallback(String path, Cmd cmd, Job job) {
        NodeTree tree = jobNodeService.get(job.getId());
        Node node = tree.find(path);
        Node next = tree.next(path);

        // bottom up recursive update node result
        NodeResult nodeResult = nodeResultService.update(job, node, cmd);
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
        if (nodeResult.isFailure()) {
            if (node instanceof Step) {
                Step step = (Step) node;
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
    }

    @Override
    public void enterQueue(CmdCallbackQueueItem cmdQueueItem) {
        try {
            cmdBaseBlockingQueue.put(cmdQueueItem);
        } catch (Throwable throwable) {
            LOGGER.warnMarker("enterQueue", "exception - %s", throwable);
        }
    }

    @Override
    public Job stopJob(String path, Integer buildNumber) {
        Job runningJob = find(path, buildNumber);
        NodeResult result = runningJob.getRootResult();

        if (result == null) {
            throw new NotFoundException("running job not found node result - " + path);
        }

        // do not handle job since it is not in running status
        if (result.isRunning()) {
            try {
                updateNodeResult(runningJob, NodeStatus.STOPPED);
                stopJob(runningJob);
            } catch (Throwable throwable) {
                String message = "stop job error - " + ExceptionUtil.findRootCause(throwable);
                LOGGER.traceMarker("stopJob", message);
                throw new IllegalParameterException(message);
            }
        }

        return runningJob;
    }


    /**
     * Update job status by root node result
     */
    private NodeResult updateJobStatus(Job job) {
        NodeResult rootResult = nodeResultService.find(job.getNodePath(), job.getId());

        if (rootResult.isFailure()) {
            job.setStatus(JobStatus.ERROR);
        }

        if (rootResult.isSuccess()) {
            job.setStatus(JobStatus.SUCCESS);
        }

        if (rootResult.isStop()) {
            job.setStatus(JobStatus.STOPPED);
        }

        jobDao.update(job);
        return rootResult;
    }

    /**
     * Update job status and delete agent session
     */
    private void stopJob(Job job) {
        updateJobStatus(job);
        cmdService.deleteSession(job);
    }

    private void updateNodeResult(Job job, NodeStatus status) {
        List<NodeResult> results = nodeResultService.list(job);
        for (NodeResult result : results) {
            if (result.getStatus() != NodeStatus.SUCCESS) {
                result.setStatus(status);
                nodeResultService.save(result);

                if (job.getNodePath().equals(result.getPath())) {
                    job.setRootResult(result);
                }
            }
        }
    }
}
