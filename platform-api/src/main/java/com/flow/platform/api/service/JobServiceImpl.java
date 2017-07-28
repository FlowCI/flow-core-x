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

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeStatus;
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
import java.io.UnsupportedEncodingException;
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

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${platform.zone}")
    private String zone;

    @Value(value = "${platform.cmd.url}")
    private String cmdUrl;

    @Value(value = "${platform.queue.url}")
    private String queueUrl;

    private final Map<String, Job> mocJobList = new HashMap<>();

    @Override
    public Job createJob(String nodePath) {
        Job job = new Job();
        //create job node
        JobFlow jobFlow = jobNodeService.createJobNode(nodePath);
        //update job status
        job.setId(UUID.randomUUID().toString());
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
            Job job = find(id);
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
        cmdInfo.setInputs(node.getEnvs());
        cmdInfo.setWebhook(getNodeHook(node));
        JobFlow jobFlow = (JobFlow) NodeUtil.findRootNode(node);
        cmdInfo.setSessionId(jobFlow.getJob().getSessionId());
        LOGGER.traceMarker("run", String.format("stepName - %s, nodePath - %s", node.getName(), node.getPath()));
        try {
            String res = HttpUtil.post(cmdUrl, cmdInfo.toJson());

            if (res == null) {
                LOGGER.warn(String.format("post cmd error, cmdUrl: %s, cmdInfo: %s", cmdUrl, cmdInfo.toJson()));
                throw new RuntimeException(
                    String.format("post cmd error, cmdUrl: %s, cmdInfo: %s", cmdUrl, cmdInfo.toJson()));
            }

            Cmd cmd = Jsonable.parse(res, Cmd.class);
            // record cmd id
            node.setCmdId(cmd.getId());
            jobNodeService.save(node);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("run step UnsupportedEncodingException", e);
        }
    }

    @Override
    public Job save(Job job) {
        mocJobList.put(job.getId(), job);
        return job;
    }

    @Override
    public Job find(String id) {
        return mocJobList.get(id);
    }

    @Override
    public Job update(Job job) {
        mocJobList.put(job.getId(), job);
        return job;
    }

    /**
     * get job callback
     */
    private String getJobHook(Job job) {
        return domain + "/hooks?identifier=" + UrlUtil.urlEncoder(job.getId());
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
        save(job);
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
        StringBuffer stringBuffer = new StringBuffer(queueUrl);
        stringBuffer.append("?priority=1&retry=5");
        try {
            String res = HttpUtil.post(stringBuffer.toString(), cmdInfo.toJson());

            if (res == null) {
                LOGGER.warn(
                    String.format("post session to queue error, cmdUrl: %s, cmdInfo: %s", stringBuffer.toString(),
                        cmdInfo.toJson()));
                throw new RuntimeException(
                    String.format("post session to queue error, cmdUrl: %s, cmdInfo: %s", stringBuffer.toString(),
                        cmdInfo.toJson()));
            }

            cmd = Jsonable.parse(res, Cmd.class);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("run step UnsupportedEncodingException", e);
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
                throw new RuntimeException("not found job flow " + job.getNodePath());
            }

            // start run flow
            run((JobNode) NodeUtil.first(jobFlow));
        } else {
            throw new RuntimeException("create session error");
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
    private void updateJobStatus(JobFlow jobFlow) {
        Job job = jobFlow.getJob();
        job.setUpdatedAt(ZonedDateTime.now());
        job.setExitCode(jobFlow.getExitCode());
        NodeStatus nodeStatus = jobFlow.getStatus();

        if (nodeStatus == NodeStatus.TIMEOUT || nodeStatus == NodeStatus.FAILURE) {
            nodeStatus = NodeStatus.FAILURE;
        }

        job.setStatus(nodeStatus);
        save(job);

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

        if (jobNode instanceof JobFlow) {
            updateJobStatus((JobFlow) jobNode);
        }
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
    public List<JobStep> listJobStep(String jobId) {
        Job job = find(jobId);
        if (job == null) {
            throw new RuntimeException("not found job");
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
