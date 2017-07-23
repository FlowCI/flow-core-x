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
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.RestClient;
import com.flow.platform.api.util.RestClient.HttpMethod;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import java.io.IOException;
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

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private Channel createSessionChannel;

    @Value(value = "${rabbitmq.routeKey}")
    private String routeKey;

    @Value(value = "${rabbitmq.exchange}")
    private String exchange;

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${platform.zone}")
    private String zone;

    @Value(value = "${platform.cmd.url}")
    private String cmdUrl;

    private final Map<String, Job> mocJobList = new HashMap<>();

    @Override
    public Job createJob(String flowPath) {
        Job job = new Job();
        //create job node
        JobFlow jobFlow = createJobNode(flowPath);
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
        if(cmdBase.getType() == CmdType.CREATE_SESSION){
            Job job = find(id);
            if(job == null){
                throw new RuntimeException("job not found");
            }
            sessionCallback(job, cmdBase);
        }else if(cmdBase.getType() == CmdType.RUN_SHELL){
            nodeCallback(id, cmdBase);
        }else{
            throw new RuntimeException("not found cmdType");
        }
    }

    @Override
    public void run(JobNode node) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.RUN_SHELL, node.getScript());
        cmdInfo.setWebhook(getNodeHook(node));
        taskExecutor.execute(new RestClient(HttpMethod.POST, cmdUrl, cmdInfo.toJson()));
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
     * @param job
     * @return
     */
    private String getJobHook(Job job){
        return domain + "/callback/job/" + job.getId();
    }

    /**
     * get node callback
     * @param node
     * @return
     */
    private String getNodeHook(Node node){
        return domain + "/callback/node/" + node.getPath();
    }

    /**
     * create session
     * @param job
     */
    private void createSession(Job job){
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobHook(job));
        mqPublish(cmdInfo.toBytes());
    }

    /**
     * delete sessionId
     * @param job
     */
    private void deleteSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());
        mqPublish(cmdInfo.toBytes());
    }

    /**
     * publish msg to rabbitmq
     * @param bytes
     */
    private void mqPublish(byte[] bytes){
        try {
            createSessionChannel.basicPublish(exchange, routeKey, null, bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * copy node to job node and save
     * @param flowPath
     * @return
     */
    @Override
    public JobFlow createJobNode(String flowPath) {
        Node flow = nodeService.find(flowPath);
        NodeUtil.recurse(flow, node -> {
            JobNode jobNode;
            if (node instanceof Flow) {
                jobNode = copyNode(node, Flow.class, JobFlow.class);
            } else {
                jobNode = copyNode(node, Step.class, JobStep.class);
                ;
            }
            jobNodeService.save(jobNode);
            for (Node child : node.getChildren()) {
                JobNode jobChild = (JobNode) jobNodeService.find(child.getPath());
                jobNode.getChildren().add(jobChild);
                jobChild.setParent(jobNode);
                jobNodeService.save(jobChild);
            }
        });
        return (JobFlow) jobNodeService.find(flow.getPath());
    }

    /**
     * copy node data to job node
     * @param node
     * @param sourceClass
     * @param targetClass
     * @return
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
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return (JobNode) k;
    }

    /**
     * session success callback
     * @param job
     * @param cmdBase
     */
    private void sessionCallback(Job job, CmdBase cmdBase){
        if(cmdBase.getStatus() == CmdStatus.SENT){
            job.setUpdatedAt(ZonedDateTime.now());
            job.setSessionId(cmdBase.getSessionId());
            update(job);
            // run step
            JobFlow jobFlow = (JobFlow) jobNodeService.find(job.getNodePath());
            if(jobFlow == null){
                throw new RuntimeException("not found job flow " + job.getNodePath());
            }

            // start run flow
            run((JobNode) NodeUtil.next(jobFlow));
        }else{
            throw new RuntimeException("create session error");
        }
    }

    /**
     * step success callback
     * @param nodePath
     * @param cmdBase
     */
    private void nodeCallback(String nodePath, CmdBase cmdBase){
        JobStep jobStep = (JobStep) jobNodeService.find(nodePath);
        JobStep prevStep = (JobStep) NodeUtil.prev(jobStep);
        JobFlow jobFlow = (JobFlow) NodeUtil.findRootNode(jobStep);
        Job job = jobFlow.getJob();
        //update job step status
        jobStep = updateJobStepStatus(jobStep, cmdBase);

        switch (jobStep.getStatus()){
            case PENDING:
            case RUNNING:
                // first step
                if(prevStep == null){
                    updateJobAndFlowStatus(jobFlow, jobStep, job);
                }
                break;
            case SUCCESS:
                JobStep nextStep = (JobStep) NodeUtil.next(jobStep);
                if(nextStep == null){
                    updateJobAndFlowStatus(jobFlow, jobStep, job);
                }else{
                    run(nextStep);
                }
                break;

            case TIMEOUT:
            case FAIL:
                updateJobAndFlowStatus(jobFlow, jobStep, job);
                break;
        }
    }

    /**
     * update job flow status
     * @param jobFlow
     * @param jobStep
     * @param job
     * @return
     */
    private JobFlow updateJobAndFlowStatus(JobFlow jobFlow, JobStep jobStep, Job job){
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
     * @param jobStep
     * @param cmdBase
     * @return
     */
    private JobStep updateJobStepStatus(JobStep jobStep, CmdBase cmdBase){
        //update jobStep
        jobStep.setUpdatedAt(ZonedDateTime.now());
        jobStep.setStatus(handleStatus(cmdBase));
        CmdResult cmdResult = ((Cmd)cmdBase).getCmdResult();
        if(cmdResult != null){
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
     * @param cmdBase
     * @return
     */
    private NodeStatus handleStatus(CmdBase cmdBase){
        NodeStatus nodeStatus = null;
        switch (cmdBase.getStatus()){
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
                nodeStatus = NodeStatus.FAIL;
                break;
            case TIMEOUT_KILL:
                nodeStatus = NodeStatus.TIMEOUT;
                break;
        }
        return nodeStatus;
    }

}
