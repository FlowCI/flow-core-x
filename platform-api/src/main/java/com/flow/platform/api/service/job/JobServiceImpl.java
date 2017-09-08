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
import com.flow.platform.api.dao.job.NodeResultDao;
import com.flow.platform.api.domain.CmdQueueItem;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.domain.Cmd;
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

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private BlockingQueue<CmdQueueItem> cmdBaseBlockingQueue;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private PlatformURL platformURL;

    @Override
    public Job find(BigInteger id) {
        return jobDao.get(id);
    }

    @Override
    public Job find(String flowName, Integer number) {
        Job job = jobDao.get(flowName, number);
        if (job == null) {
            throw new NotFoundException("job is not found");
        }
        return job;
    }

    @Override
    public List<NodeResult> listNodeResult(String path, Integer number) {
        Job job = find(path, number);
        return nodeResultService.list(job);
    }

    @Override
    public List<Job> list(List<String> paths, boolean latestOnly) {
        if (latestOnly) {
            jobDao.latestByPath(paths);
        }

        return jobDao.listByPath(paths);
    }

    @Override
    public Job createJob(String path) {
        Node root = nodeService.find(PathUtil.rootPath(path));
        if (root == null) {
            throw new IllegalParameterException("Path does not existed");
        }

        String status = root.getEnv(FlowEnvs.FLOW_STATUS);
        if (Strings.isNullOrEmpty(status) || !status.equals(FlowEnvs.StatusValue.READY.value())) {
            throw new IllegalStatusException("Cannot createOrUpdate job since status is not READY");
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

        // createOrUpdate job
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath(root.getPath());
        job.setNodeName(root.getName());
        job.setNumber(jobDao.maxBuildNumber(job.getNodePath()) + 1);
        job.setEnvs(root.getEnvs());

        //save job
        jobDao.save(job);

        // createOrUpdate yml snapshot for job
        jobNodeService.save(job.getId(), yml);

        // init for node result
        NodeResult rootResult = nodeResultService.create(job);
        job.setResult(rootResult);

        // to createOrUpdate agent session for job
        String sessionId = cmdService.createSession(job);
        job.setSessionId(sessionId);
        job.setStatus(JobStatus.SESSION_CREATING);
        jobDao.update(job);

        return job;
    }

    @Override
    public void callback(CmdQueueItem cmdQueueItem) {
        BigInteger jobId = cmdQueueItem.getJobId();
        Cmd cmd = cmdQueueItem.getCmd();
        Job job = find(jobId);

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

        String cmdId = cmdService.runShell(job, node);
        NodeResult nodeResult = nodeResultService.find(node.getPath(), job.getId());

        // record cmd id
        nodeResult.setCmdId(cmdId);
        nodeResultService.save(nodeResult);
    }

    /**
     * Create session callback
     */
    private void onCreateSessionCallback(Job job, Cmd cmd) {
        if (cmd.getStatus() != CmdStatus.SENT) {
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

        NodeResult nodeResult = nodeResultService.update(job, node, cmd);

        // no more node to run or manual stop node, update job data
        if (next == null || nodeResult.isStop()) {
            String rootPath = PathUtil.rootPath(path);
            NodeResult rootResult = nodeResultService.find(rootPath, job.getId());

            // update job status
            updateJobStatus(job, rootResult);
            LOGGER.debug("The node tree '%s' been executed with %s status", rootPath, rootResult.getStatus());

            // send to delete session
            if (!nodeResult.isRunning()) {
                cmdService.deleteSession(job);
            }
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
            }
        }
    }

    @Override
    public void enterQueue(CmdQueueItem cmdQueueItem) {
        try {
            cmdBaseBlockingQueue.put(cmdQueueItem);
        } catch (Throwable throwable) {
            LOGGER.warnMarker("enterQueue", "exception - %s", throwable);
        }
    }

    @Override
    public Job stopJob(String path, Integer buildNumber) {
        Job runningJob = find(path, buildNumber);
        NodeResult result = runningJob.getResult();

        if (runningJob == null) {
            throw new NotFoundException("running job not found by path - " + path);
        }

        if (result == null) {
            throw new NotFoundException("running job not found node result - " + path);
        }

        // do not handle job since it is not in running status
        if (result.isRunning()) {
            try {
                cmdService.deleteSession(runningJob);
                updateNodeResult(runningJob, NodeStatus.STOPPED);
                updateJobStatus(runningJob, result);
            } catch (Throwable throwable) {
                String message = "stop job error - " + ExceptionUtil.findRootCause(throwable);
                LOGGER.traceMarker("stopJob", message);
                throw new IllegalParameterException(message);
            }
        }

        return runningJob;
    }

    private void updateJobStatus(Job job, NodeResult rootResult) {
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
    }

    private void updateNodeResult(Job job, NodeStatus status) {
        List<NodeResult> results = nodeResultService.list(job);
        for (NodeResult result : results) {
            if (result.getStatus() != NodeStatus.SUCCESS) {
                result.setStatus(status);
                nodeResultService.save(result);

                if (job.getNodePath().equals(result.getPath())) {
                    job.setResult(result);
                }
            }
        }
    }
}
