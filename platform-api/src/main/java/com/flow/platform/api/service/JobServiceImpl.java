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
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.exception.HttpException;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.HttpUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectUtil;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
public class JobServiceImpl implements JobService {

    private static Logger LOGGER = new Logger(JobService.class);

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private JobDao jobDao;

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${platform.zone}")
    private String zone;

    @Value(value = "${platform.cmd.url}")
    private String cmdUrl;

    @Value(value = "${platform.queue.url}")
    private String queueUrl;

    @Override
    public Job createJob(String nodePath) {
        Job job = new Job(CommonUtil.randomId());
        //create job node
        JobFlow jobFlow = (JobFlow) jobNodeService.createJobNode(nodePath);
        //update job status
        job.setId(CommonUtil.randomId());
        job.setCreatedAt(ZonedDateTime.now());
        job.setUpdatedAt(ZonedDateTime.now());
        job.setNodePath(jobFlow.getPath());
        job.setStatus(NodeStatus.PENDING);
        save(job);
        jobFlow.setJob(job);
        jobNodeService.save(jobFlow);
        //create session
        createSession(job);
        return job;
    }

    @Override
    public void callback(String id, CmdBase cmdBase) {
        if (cmdBase.getType() == CmdType.CREATE_SESSION) {
            Job job = find(new BigInteger(id));
            if (job == null) {
                LOGGER.warn(String.format("job not found, jobId: %s", id));
                throw new RuntimeException("job not found");
            }
            sessionCallback(job, cmdBase);
        } else if (cmdBase.getType() == CmdType.RUN_SHELL) {
            nodeCallback(id, cmdBase);
        } else {
            LOGGER.warn(String.format("not found cmdType, cmdType: %s", cmdBase.getType().toString()));
            throw new RuntimeException("not found cmdType");
        }
    }

    /**
     * run node
     *
     * @param node job node's script and record cmdId and sync send http
     */
    @Override
    public void run(JobNode node) {
        if (!NodeUtil.canRun(node)) {
            // run next node
            run((JobNode) NodeUtil.next(node));
            return;
        }

        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.RUN_SHELL, node.getScript());
        JobFlow jobFlow = (JobFlow) NodeUtil.findRootNode(node);
        cmdInfo.setInputs(mergeEnvs(jobFlow.getEnvs(), node.getEnvs()));
        cmdInfo.setWebhook(getNodeHook(node));

        Job job = find(jobFlow.getJob().getId());
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
            // record cmd id
            node.setCmdId(cmd.getId());
            jobNodeService.save(node);
        } catch (Throwable ignore) {
            LOGGER.warn("run step UnsupportedEncodingException", ignore);
        }
    }

    /**
     * merge two env
     */
    private Map<String, String> mergeEnvs(Map<String, String> flowEnv, Map<String, String> stepEnv) {
        Map<String, String> hash = new HashMap<>();
        if (flowEnv != null) {
            hash.putAll(flowEnv);
        }

        if (stepEnv != null) {
            hash.putAll(stepEnv);
        }

        return hash;
    }

    @Override
    public Job save(Job job) {
        jobDao.save(job);
        return job;
    }

    @Override
    public Job find(BigInteger id) {
        return jobDao.get(id);
    }

    @Override
    public Job update(Job job) {
        jobDao.update(job);
        return job;
    }

    /**
     * get job callback
     */
    private String getJobHook(Job job) {
        return domain + "/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString());
    }

    /**
     * get node callback
     */
    private String getNodeHook(Node node) {
        return domain + "/hooks?identifier=" + UrlUtil.urlEncoder(node.getPath());
    }

    /**
     * create session
     */
    private void createSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.CREATE_SESSION, null);
        LOGGER.traceMarker("createSession", String.format("jobId - %s", job.getId()));
        cmdInfo.setWebhook(getJobHook(job));
        // create session
        Cmd cmd = sendToQueue(cmdInfo);
        //enter queue
        job.setStatus(NodeStatus.ENQUEUE);
        job.setCmdId(cmd.getId());
        update(job);
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
            update(job);
            // run step
            JobFlow jobFlow = (JobFlow) jobNodeService.find(job.getNodePath());
            if (jobFlow == null) {
                throw new NotFoundException(String.format("Not Found Job Flow - %s", jobFlow.getName()));
            }

            // start run flow
            run((JobNode) NodeUtil.first(jobFlow));
        } else {
            LOGGER.warn(String.format("Create Session Error Session Status - %s", cmdBase.getStatus().getName()));
        }
    }

    /**
     * step success callback
     */
    private void nodeCallback(String nodePath, CmdBase cmdBase) {
        JobStep jobStep = (JobStep) jobNodeService.find(nodePath);
        NodeStatus nodeStatus = handleStatus(cmdBase);

        // keep job step status sorted
        if (jobStep.getStatus().getLevel() > nodeStatus.getLevel()) {
            return;
        }

        //update job step status
        jobStep.setStatus(nodeStatus);

        //update node status
        updateNodeStatus(jobStep, cmdBase);
    }

    /**
     * update job flow status
     */
    private void updateJobStatus(JobNode jobNode) {
        Job job = null;
        if(jobNode.getJob() != null){
            job = find(jobNode.getJob().getId());
        }
        if (job == null) {
            return;
        }
        job.setUpdatedAt(ZonedDateTime.now());
        job.setExitCode(jobNode.getExitCode());
        NodeStatus nodeStatus = jobNode.getStatus();

        if (nodeStatus == NodeStatus.TIMEOUT || nodeStatus == NodeStatus.FAILURE) {
            nodeStatus = NodeStatus.FAILURE;
        }

        job.setStatus(nodeStatus);
        update(job);

        //delete session
        if (nodeStatus == NodeStatus.FAILURE || nodeStatus == NodeStatus.SUCCESS) {
            deleteSession(job);
        }
    }

    /**
     * update node status
     *
     * @param jobNode node
     */
    private void updateNodeStatus(JobNode jobNode, CmdBase cmdBase) {
        //update jobNode
        jobNode.setUpdatedAt(ZonedDateTime.now());
        jobNode.setStatus(handleStatus(cmdBase));
        CmdResult cmdResult = ((Cmd) cmdBase).getCmdResult();

        if (cmdResult != null) {
            jobNode.setExitCode(cmdResult.getExitValue());
            if (NodeUtil.canRun(jobNode)) {
                jobNode.setDuration(cmdResult.getDuration());
                jobNode.setOutputs(cmdResult.getOutput());
                jobNode.setLogPaths(((Cmd) cmdBase).getLogPaths());
                jobNode.setStartTime(cmdResult.getStartTime());
                jobNode.setFinishTime(((Cmd) cmdBase).getFinishedDate());
            }
        }

        Node parent = jobNode.getParent();
        Node prev = jobNode.getPrev();
        Node next = jobNode.getNext();
        switch (jobNode.getStatus()) {
            case PENDING:
            case RUNNING:
                if (cmdResult != null) {
                    jobNode.setStartTime(cmdResult.getStartTime());
                }

                if (parent != null) {
                    // first node running update parent node running
                    if (prev == null) {
                        updateNodeStatus((JobNode) jobNode.getParent(), cmdBase);
                    }
                }
                break;
            case SUCCESS:
                if (cmdResult != null) {
                    jobNode.setFinishTime(cmdResult.getFinishTime());
                }

                if (parent != null) {
                    // last node running update parent node running
                    if (next == null) {
                        updateNodeStatus((JobNode) jobNode.getParent(), cmdBase);
                    } else {
                        run((JobNode) NodeUtil.next(jobNode));
                    }
                }
                break;
            case TIMEOUT:
            case FAILURE:
                if (cmdResult != null) {
                    jobNode.setFinishTime(cmdResult.getFinishTime());
                }

                //update parent node if next is not null, if allow failure is false
                if (parent != null && ((JobStep) jobNode).getAllowFailure()) {
                    if (next == null) {
                        updateNodeStatus((JobNode) jobNode.getParent(), cmdBase);
                    }
                }

                //update parent node if next is not null, if allow failure is false
                if (parent != null && !((JobStep) jobNode).getAllowFailure()) {
                    updateNodeStatus((JobNode) jobNode.getParent(), cmdBase);
                }

                //next node not null, run next node
                if (next != null && ((JobStep) jobNode).getAllowFailure()) {
                    run((JobNode) NodeUtil.next(jobNode));
                }
                break;
        }

        //update job status
        updateJobStatus(jobNode);

        //save
        jobNodeService.save(jobNode);
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
            case STOPPED:
                nodeStatus = NodeStatus.FAILURE;
                break;
            case TIMEOUT_KILL:
                nodeStatus = NodeStatus.TIMEOUT;
                break;
        }
        return nodeStatus;
    }

    @Override
    public List<JobStep> listJobStep(BigInteger jobId) {
        Job job = find(jobId);
        if (job == null) {
            throw new NotFoundException(String.format("Not Found Job - %s", job.getId()));
        }
        JobNode jobFlow = jobNodeService.find(job.getNodePath());
        List<JobStep> jobSteps = new LinkedList<>();
        for (Object node : jobFlow.getChildren()) {
            if (node instanceof JobStep) {
                JobStep js = (JobStep) node;
                JobStep jobStep = ObjectUtil.deepCopy(js);
                jobStep.setParent(null);
                jobStep.setPrev(null);
                jobStep.setNext(null);
                jobStep.setStatus(js.getStatus());
                jobStep.setChildren(new ArrayList<>());
                jobSteps.add(jobStep);
            }
        }
        return jobSteps;
    }
}
