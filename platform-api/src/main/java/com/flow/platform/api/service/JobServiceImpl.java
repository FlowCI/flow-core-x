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
package com.flow.platform.api.service;

import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.domain.CmdQueueItem;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.HttpException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private JobNodeResultService jobNodeResultService;

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

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${platform.zone}")
    private String zone;

    @Value(value = "${platform.cmd.url}")
    private String cmdUrl;

    @Value(value = "${platform.queue.url}")
    private String queueUrl;

    @Value(value = "${platform.cmd.stop.url}")
    private String cmdStopUrl;

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
            yml = nodeService.getYmlContent(root.getPath());
            if (Strings.isNullOrEmpty(yml)) {
                throw new IllegalStatusException("Yml is loading for path " + path);
            }
        } catch (FlowException e) {
            LOGGER.error("Fail to get yml content", e);
            throw e;
        }

        // create job
        Job job = new Job(CommonUtil.randomId());
        job.setStatus(NodeStatus.PENDING);
        job.setNodePath(root.getPath());
        job.setNodeName(root.getName());
        job.setNumber(jobDao.maxBuildNumber(job.getNodeName()) + 1);
        job.setOutputs(root.getEnvs());

        //save job
        jobDao.save(job);

        // create yml snapshot for job
        jobNodeService.save(job.getId(), yml);

        // init for node result
        jobNodeResultService.create(job);

        //create session
        createSession(job);

        return job;
    }

    @Override
    public void callback(CmdQueueItem cmdQueueItem) {
        String id = cmdQueueItem.getIdentifier();
        CmdBase cmdBase = cmdQueueItem.getCmdBase();
        Job job;
        if (cmdBase.getType() == CmdType.CREATE_SESSION) {

            // TODO: refactor to find(id, timeout)
            job = find(new BigInteger(id));
            if (job == null) {
                if (cmdQueueItem.getRetryTimes() < RETRY_TIMEs) {
                    try {
                        Thread.sleep(1000);
                        LOGGER.traceMarker("callback", String
                            .format("job not found, retry times - %s jobId - %s", cmdQueueItem.getRetryTimes(), id));
                    } catch (Throwable throwable) {
                    }

                    cmdQueueItem.plus();
                    enterQueue(cmdQueueItem);
                    return;
                }
                LOGGER.warn(String.format("job not found, jobId: %s", id));
                throw new NotFoundException("job not found");
            }
            sessionCallback(job, cmdBase);
        } else if (cmdBase.getType() == CmdType.RUN_SHELL) {
            Map<String, String> map = Jsonable.GSON_CONFIG.fromJson(id, Map.class);
            job = find(new BigInteger(map.get("jobId")));
            nodeCallback(map.get("path"), cmdBase, job);
        } else {
            LOGGER.warn(String.format("not found cmdType, cmdType: %s", cmdBase.getType().toString()));
            throw new NotFoundException("not found cmdType");
        }
    }

    /**
     * run node
     *
     * @param node job node's script and record cmdId and sync send http
     */
    @Override
    public void run(Node node, BigInteger jobId) {
        if (!NodeUtil.canRun(node)) {
            // run next node
            run(NodeUtil.next(node), jobId);
            return;
        }

        Node flow = NodeUtil.findRootNode(node);
        EnvUtil.merge(flow, node, false);

        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.RUN_SHELL, node.getScript());
        cmdInfo.setInputs(node.getEnvs());
        cmdInfo.setWebhook(getNodeHook(node, jobId));
        cmdInfo.setOutputEnvFilter("FLOW_");
        Job job = find(jobId);
        cmdInfo.setSessionId(job.getSessionId());
        LOGGER.traceMarker("run", String.format("stepName - %s, nodePath - %s", node.getName(), node.getPath()));

        try {
            String res = HttpUtil.post(cmdUrl, cmdInfo.toJson());

            if (res == null) {
                LOGGER.warn(String.format("post cmd error, cmdUrl: %s, cmdInfo: %s", cmdUrl, cmdInfo.toJson()));
                throw new HttpException(
                    String.format("Post Cmd Error, Node Name - %s, CmdInfo - %s", node.getName(), cmdInfo.toJson()));
            }

            Cmd cmd = Jsonable.parse(res, Cmd.class);
            NodeResult nodeResult = jobNodeResultService.find(node.getPath(), jobId);

            // record cmd id
            nodeResult.setCmdId(cmd.getId());
            jobNodeResultService.update(nodeResult);
        } catch (Throwable ignore) {
            LOGGER.warn("run step UnsupportedEncodingException", ignore);
        }
    }

    @Override
    public Job find(BigInteger id) {
        return jobDao.get(id);
    }

    /**
     * get job callback
     */
    private String getJobHook(Job job) {
        return domain + "/hooks/cmd?identifier=" + UrlUtil.urlEncoder(job.getId().toString());
    }

    /**
     * get node callback
     */
    private String getNodeHook(Node node, BigInteger jobId) {
        Map<String, String> map = new HashMap<>();
        map.put("path", node.getPath());
        map.put("jobId", jobId.toString());
        return domain + "/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map));
    }

    /**
     * Send create session cmd to create session
     *
     * @throws IllegalStatusException when cannot get Cmd obj from cc
     */
    private void createSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobHook(job));
        LOGGER.traceMarker("createSession", String.format("jobId - %s", job.getId()));

        // create session
        Cmd cmd = sendToQueue(cmdInfo);
        if (cmd == null) {
            throw new IllegalStatusException("Unable to create session since cmd return null");
        }

        //enter queue
        job.setStatus(NodeStatus.ENQUEUE);
        job.setCmdId(cmd.getId());
        jobDao.update(job);
    }

    /**
     * delete sessionId
     */
    private void deleteSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());

        LOGGER.traceMarker("deleteSession", String.format("sessionId - %s", job.getSessionId()));
        // delete session
        sendToQueue(cmdInfo);
    }

    /**
     * send cmd by queue
     */
    private Cmd sendToQueue(CmdInfo cmdInfo) {
        Cmd cmd = null;
        StringBuilder stringBuilder = new StringBuilder(queueUrl);
        stringBuilder.append("?priority=1&retry=5");
        try {
            String res = HttpUtil.post(stringBuilder.toString(), cmdInfo.toJson());

            if (res == null) {
                String message = String
                    .format("post session to queue error, cmdUrl: %s, cmdInfo: %s", stringBuilder.toString(),
                        cmdInfo.toJson());

                LOGGER.warn(message);
                throw new HttpException(message);
            }

            cmd = Jsonable.parse(res, Cmd.class);
        } catch (Throwable ignore) {
            LOGGER.warn("run step UnsupportedEncodingException", ignore);
        }
        return cmd;
    }

    /**
     * session success callback
     */
    private void sessionCallback(Job job, CmdBase cmdBase) {
        if (cmdBase.getStatus() == CmdStatus.SENT) {
            job.setUpdatedAt(ZonedDateTime.now());
            job.setSessionId(cmdBase.getSessionId());
            jobDao.update(job);

            // run step
            NodeResult nodeResult = jobNodeResultService.find(job.getNodePath(), job.getId());
            Node flow = jobNodeService.get(job.getId(), nodeResult.getNodeResultKey().getPath());

            if (flow == null) {
                throw new NotFoundException(String.format("Not Found Job Flow - %s", flow.getName()));
            }

            // start run flow
            run(NodeUtil.first(flow), job.getId());
        } else {
            LOGGER.warn(String.format("Create Session Error Session Status - %s", cmdBase.getStatus().getName()));
        }
    }

    /**
     * step success callback
     */
    private void nodeCallback(String nodePath, CmdBase cmdBase, Job job) {
        NodeResult nodeResult = jobNodeResultService.find(nodePath, job.getId());
        NodeStatus nodeStatus = handleStatus(cmdBase);

        // keep job step status sorted
        if (nodeResult.getStatus().getLevel() >= nodeStatus.getLevel()) {
            return;
        }

        //update job step status
        nodeResult.setStatus(nodeStatus);

        jobNodeResultService.update(nodeResult);

        Node step = jobNodeService.get(job.getId(), nodeResult.getNodeResultKey().getPath());
        //update node status
        updateNodeStatus(step, cmdBase, job);
    }

    /**
     * update job flow status
     */
    private void updateJobStatus(NodeResult nodeResult) {
        Node node = jobNodeService
            .get(nodeResult.getNodeResultKey().getJobId(), nodeResult.getNodeResultKey().getPath());
        Job job = find(nodeResult.getNodeResultKey().getJobId());

        if (node instanceof Step) {
            //merge step outputs in flow outputs
            EnvUtil.merge(nodeResult.getOutputs(), job.getOutputs(), false);
            job.setDuration(job.getDuration() + nodeResult.getDuration());
            jobDao.update(job);
            return;
        }

        job.setUpdatedAt(ZonedDateTime.now());
        job.setExitCode(nodeResult.getExitCode());
        NodeStatus nodeStatus = nodeResult.getStatus();

        if (nodeStatus == NodeStatus.TIMEOUT || nodeStatus == NodeStatus.FAILURE) {
            nodeStatus = NodeStatus.FAILURE;
        }

        job.setStatus(nodeStatus);
        jobDao.update(job);

        //delete session
        if (nodeStatus == NodeStatus.FAILURE || nodeStatus == NodeStatus.SUCCESS) {

            deleteSession(job);
        }
    }

    /**
     * update node status
     */
    private void updateNodeStatus(Node node, CmdBase cmdBase, Job job) {
        NodeResult nodeResult = jobNodeResultService.find(node.getPath(), job.getId());
        //update jobNode
        nodeResult.setUpdatedAt(ZonedDateTime.now());
        nodeResult.setStatus(handleStatus(cmdBase));
        CmdResult cmdResult = ((Cmd) cmdBase).getCmdResult();

        if (cmdResult != null) {
            nodeResult.setExitCode(cmdResult.getExitValue());
            if (NodeUtil.canRun(node)) {
                if (cmdResult.getDuration() != null) {
                    nodeResult.setDuration(cmdResult.getDuration());
                }
                nodeResult.setOutputs(cmdResult.getOutput());
                nodeResult.setLogPaths(((Cmd) cmdBase).getLogPaths());
                nodeResult.setStartTime(cmdResult.getStartTime());
                nodeResult.setFinishTime(((Cmd) cmdBase).getFinishedDate());
            }
        }

        Node parent = node.getParent();
        Node prev = node.getPrev();
        Node next = node.getNext();
        switch (nodeResult.getStatus()) {
            case PENDING:
            case RUNNING:
                if (cmdResult != null) {
                    nodeResult.setStartTime(cmdResult.getStartTime());
                }

                if (parent != null) {
                    // first node running update parent node running
                    if (prev == null) {
                        updateNodeStatus(node.getParent(), cmdBase, job);
                    }
                }
                break;
            case SUCCESS:
                if (cmdResult != null) {
                    nodeResult.setFinishTime(cmdResult.getFinishTime());
                }

                if (parent != null) {
                    // last node running update parent node running
                    if (next == null) {
                        updateNodeStatus(node.getParent(), cmdBase, job);
                    } else {
                        run(NodeUtil.next(node), job.getId());
                    }
                }
                break;
            case TIMEOUT:
            case FAILURE:
                if (cmdResult != null) {
                    nodeResult.setFinishTime(cmdResult.getFinishTime());
                }

                //update parent node if next is not null, if allow failure is false
                if (parent != null && (((Step) node).getAllowFailure())) {
                    if (next == null) {
                        updateNodeStatus(node.getParent(), cmdBase, job);
                    }
                }

                //update parent node if next is not null, if allow failure is false
                if (parent != null && !((Step) node).getAllowFailure()) {
                    updateNodeStatus(node.getParent(), cmdBase, job);
                }

                //next node not null, run next node
                if (next != null && ((Step) node).getAllowFailure()) {
                    run(NodeUtil.next(node), job.getId());
                }
                break;
        }

        //update job status
        updateJobStatus(nodeResult);

        //save
        jobNodeResultService.update(nodeResult);
    }

    /**
     * transfer cmdStatus to Job status
     */
    private NodeStatus handleStatus(CmdBase cmdBase) {
        NodeStatus nodeStatus = null;
        switch (cmdBase.getStatus()) {
            case SENT:
            case PENDING:
                nodeStatus = NodeStatus.PENDING;
                break;
            case RUNNING:
            case EXECUTED:
                nodeStatus = NodeStatus.RUNNING;
                break;
            case LOGGED:
                CmdResult cmdResult = ((Cmd) cmdBase).getCmdResult();
                if (cmdResult != null && cmdResult.getExitValue() == 0) {
                    nodeStatus = NodeStatus.SUCCESS;
                } else {
                    nodeStatus = NodeStatus.FAILURE;
                }
                break;
            case KILLED:
            case EXCEPTION:
            case REJECTED:
                nodeStatus = NodeStatus.FAILURE;
                break;
            case STOPPED:
                nodeStatus = NodeStatus.STOPPED;
                break;
            case TIMEOUT_KILL:
                nodeStatus = NodeStatus.TIMEOUT;
                break;
        }
        return nodeStatus;
    }

    @Override
    public List<Job> listJobs(String flowName, List<String> flowNames) {
        if (flowName == null && flowNames == null) {
            return jobDao.list();
        }

        if (flowNames != null) {
            return jobDao.listLatest(flowNames);
        }

        if (flowName != null) {
            return jobDao.list(flowName);
        }
        return null;
    }

    @Override
    public Job find(String flowName, Integer number) {
        return jobDao.get(flowName, number);
    }

    @Override
    public void enterQueue(CmdQueueItem cmdQueueItem) {
        try {
            cmdBaseBlockingQueue.put(cmdQueueItem);
        } catch (Throwable throwable) {
            LOGGER.warnMarker("enterQueue", String.format("exception - %s", throwable));
        }
    }

    @Override
    public Boolean stopJob(String name, Integer buildNumber) {
        String cmdId;
        Job runningJob = find(name, buildNumber);

        if (runningJob == null) {
            throw new NotFoundException(String.format("running job not found name - %s", name));
        }

        //job in create session status
        if (runningJob.getStatus() == NodeStatus.ENQUEUE || runningJob.getStatus() == NodeStatus.PENDING) {
            cmdId = runningJob.getCmdId();

            // job finish, stop job failure
        } else if (runningJob.getStatus() == NodeStatus.SUCCESS || runningJob.getStatus() == NodeStatus.FAILURE) {
            return false;

        } else { // running
            NodeResult runningNodeResult = nodeResultDao.get(runningJob.getId(), NodeStatus.RUNNING, NodeTag.STEP);
            cmdId = runningNodeResult.getCmdId();
        }

        String url = new StringBuilder(cmdStopUrl).append(cmdId).toString();
        LOGGER.traceMarker("stopJob", String.format("url - %s", url));

        runningJob.setStatus(NodeStatus.STOPPED);

        jobDao.update(runningJob);
        updateNodeResult(runningJob, NodeStatus.STOPPED);

        try {
            String res = HttpUtil.post(url, "");
            if (Strings.isNullOrEmpty(res)) {
                return false;
            }
            return true;
        } catch (Throwable throwable) {
            LOGGER.traceMarker("stopJob", String.format("stop job error - %s", throwable));
            return false;
        }
    }

    private void updateNodeResult(Job job, NodeStatus status) {
        List<NodeResult> results = jobNodeResultService.list(job);
        for (NodeResult result : results) {
            if (result.getStatus() != NodeStatus.SUCCESS) {
                result.setStatus(status);
                jobNodeResultService.update(result);
            }
        }
    }

    @Override
    public List<NodeResult> listNodeResult(String flowName, Integer number) {
        Job job = find(flowName, number);
        return jobNodeResultService.list(job);
    }
}
