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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.Step;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
public class JobServiceImpl implements JobService {

    private static Logger LOGGER = new Logger(JobService.class);

    @Autowired
    private NodeService nodeService;

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
        JobFlow jobFlow = createJobNode(nodePath);
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
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.RUN_SHELL, node.getScript());
        cmdInfo.setWebhook(getNodeHook(node));

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
        return domain + "/hooks/" + UrlUtil.urlEncoder(job.getId());
    }

    /**
     * get node callback
     */
    private String getNodeHook(Node node) {
        return domain + "/hooks/" + UrlUtil.urlEncoder(node.getPath());
    }

    /**
     * create session
     */
    private void createSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobHook(job));
        // create session
        Cmd cmd = sendToQueue(cmdInfo);
        job.setCmdId(cmd.getId());
        save(job);
    }

    /**
     * delete sessionId
     */
    private void deleteSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());
        // delete session
        sendToQueue(cmdInfo);
    }

    /**
     * send cmd by queue
     */
    private Cmd sendToQueue(CmdInfo cmdInfo) {
        Cmd cmd = null;
        try {
            String res = HttpUtil.post(cmdUrl, cmdInfo.toJson());

            if (res == null) {
                LOGGER.warn(
                    String.format("post session to queue error, cmdUrl: %s, cmdInfo: %s", cmdUrl, cmdInfo.toJson()));
                throw new RuntimeException(
                    String.format("post session to queue error, cmdUrl: %s, cmdInfo: %s", cmdUrl, cmdInfo.toJson()));
            }

            cmd = Jsonable.parse(res, Cmd.class);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("run step UnsupportedEncodingException", e);
        }
        return cmd;
    }

    /**
     * copy node to job node and save
     */
    @Override
    public JobFlow createJobNode(String nodePath) {
        Node flow = nodeService.find(nodePath);
        NodeUtil.recurse(flow, node -> {
            JobNode jobNode;
            if (node instanceof Flow) {
                jobNode = copyNode(node, Flow.class, JobFlow.class);
            } else {
                jobNode = copyNode(node, Step.class, JobStep.class);
            }
            jobNodeService.save(jobNode);
            for (Node child : node.getChildren()) {
                JobNode jobChild = jobNodeService.find(child.getPath());
                jobNode.getChildren().add(jobChild);
                jobChild.setParent(jobNode);
                jobNodeService.save(jobChild);
            }
        });
        return (JobFlow) jobNodeService.find(flow.getPath());
    }

    /**
     * copy node data to job node
     */
    private JobNode copyNode(Node node, Class<?> sourceClass, Class<?> targetClass) {
        Object k = null;
        try {
            k = targetClass.newInstance();
            Object finalK = k;
            ReflectionUtils.doWithFields(sourceClass, field -> {
                field.setAccessible(true);
                Object ob = ReflectionUtils.getField(field, node);
                ObjectUtil.assignValueToField(field, finalK, ob);
            });
        } catch (InstantiationException e) {
            LOGGER.warn("copy node InstantiationException %s", e);
        } catch (IllegalAccessException e) {
            LOGGER.warn("copy node IllegalAccessException %s", e);
        }
        return (JobNode) k;
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
            run((JobNode) NodeUtil.next(jobFlow));
        } else {
            throw new RuntimeException("create session error");
        }
    }

    /**
     * step success callback
     */
    private void nodeCallback(String nodePath, CmdBase cmdBase) {
        JobStep jobStep = (JobStep) jobNodeService.find(nodePath);
        JobStep prevStep = (JobStep) NodeUtil.prev(jobStep);
        JobFlow jobFlow = (JobFlow) NodeUtil.findRootNode(jobStep);
        Job job = jobFlow.getJob();
        //update job step status
        jobStep = updateJobStepStatus(jobStep, cmdBase);

        switch (jobStep.getStatus()) {
            case PENDING:
            case RUNNING:
                // first step
                if (prevStep == null) {
                    updateJobAndFlowStatus(jobFlow, jobStep, job);
                }
                break;
            case SUCCESS:
                JobStep nextStep = (JobStep) NodeUtil.next(jobStep);
                if (nextStep == null) {
                    updateJobAndFlowStatus(jobFlow, jobStep, job);
                    deleteSession(job);
                } else {
                    run(nextStep);
                }
                break;

            case TIMEOUT:
            case FAILURE:
                updateJobAndFlowStatus(jobFlow, jobStep, job);
                deleteSession(job);
                break;
        }
    }

    /**
     * update job flow status
     */
    private JobFlow updateJobAndFlowStatus(JobFlow jobFlow, JobStep jobStep, Job job) {
        job.setUpdatedAt(ZonedDateTime.now());
        job.setStatus(jobStep.getStatus());
        job.setExitCode(jobStep.getExitCode());

        jobFlow.setUpdatedAt(ZonedDateTime.now());
        jobFlow.setStatus(jobStep.getStatus());
        jobNodeService.save(jobFlow);
        save(job);
        return jobFlow;
    }

    /**
     * update job step status
     */
    private JobStep updateJobStepStatus(JobStep jobStep, CmdBase cmdBase) {
        //update jobStep
        jobStep.setUpdatedAt(ZonedDateTime.now());
        jobStep.setStatus(handleStatus(cmdBase));
        CmdResult cmdResult = ((Cmd) cmdBase).getCmdResult();
        if (cmdResult != null) {
            jobStep.setExitCode(cmdResult.getExitValue());
            jobStep.setDuration(cmdResult.getDuration());
            jobStep.setOutputs(cmdResult.getOutput());
            jobStep.setLogPaths(((Cmd) cmdBase).getLogPaths());
            jobStep.setFinishedAt(((Cmd) cmdBase).getFinishedDate());
            jobStep.setStartTime(cmdResult.getStartTime());
        }

        //save
        jobNodeService.save(jobStep);
        return jobStep;
    }

    /**
     * transfer cmdStatus to Job status
     */
    private NodeStatus handleStatus(CmdBase cmdBase) {
        NodeStatus nodeStatus = null;
        switch (cmdBase.getStatus()) {
            case PENDING:
                nodeStatus = NodeStatus.PENDING;
                break;
            case RUNNING:
                nodeStatus = NodeStatus.RUNNING;
                break;
            case LOGGED:
            case EXECUTED:
                nodeStatus = NodeStatus.SUCCESS;
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

}
