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
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.RestClient;
import com.flow.platform.api.util.RestClient.HttpMethod;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ObjectUtil;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
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

    private final Map<String, Job> mocJobList = new HashMap<>();

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeUtil nodeUtil;

    @Autowired
    private RabbitmqService rabbitmqService;

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${rabbitmq.routeKey}")
    private String routeKey;

    @Value(value = "${platform.api")
    private String platform_url;

    @Override
    public Boolean handleCmdResult(Cmd cmd, String nodePath) {
        JobNode jobNode = jobNodeService.find(nodePath);
        JobStep jobStep = (JobStep) jobNode;
        handleStatus(cmd, jobStep);
        return true;
    }

    private void handleStatus(Cmd cmd, JobStep jobStep) {
        NodeStatus nodeStatus = NodeStatus.SUCCESS;
        jobStep.setCmdId(cmd.getId());
        jobStep.setStartedTime(cmd.getResult().getStartTime());
        jobStep.setNodeStatus(nodeStatus);
        jobStep.setDuration(cmd.getResult().getDuration());
        jobStep.setExitCode(cmd.getResult().getExitValue());
        jobStep.setLogPaths(cmd.getLogPaths());
        Job job = jobStep.getJob();
        switch (cmd.getStatus()) {
            case PENDING:
                nodeStatus = NodeStatus.PENDING;
                jobStep.setNodeStatus(nodeStatus);
                break;
            case RUNNING:
                nodeStatus = NodeStatus.RUNNING;
                jobStep.setNodeStatus(nodeStatus);
                //first step
                if(nodeUtil.prevNodeFromAllChildren(jobStep) == null){
                    updateFlowStatus(jobStep, cmd, true, false);
                    updateJobStatus(jobStep, cmd, true, false);
                }

                break;
            case EXECUTED:
            case LOGGED:
                nodeStatus = NodeStatus.SUCCESS;
                jobStep.setNodeStatus(nodeStatus);
                JobStep next = (JobStep) nodeUtil.nextNodeFromAllChildren(jobStep);
                // end step
                if(next == null){
                    updateFinishStatus(jobStep, cmd, job);
                    return;
                }
                run(next);
                break;
            case REJECTED:
            case KILLED:
            case EXCEPTION:
                nodeStatus = NodeStatus.FAIL;
                jobStep.setNodeStatus(nodeStatus);
                next = (JobStep) nodeUtil.nextNodeFromAllChildren(jobStep);
                // end step
                if(next == null){
                    updateFinishStatus(jobStep, cmd, job);
                    return;
                }
                break;
            case TIMEOUT_KILL:
                nodeStatus = NodeStatus.TIMEOUT;
                jobStep.setNodeStatus(nodeStatus);
                next = (JobStep) nodeUtil.nextNodeFromAllChildren(jobStep);
                // end step
                if(next == null){
                    updateFinishStatus(jobStep, cmd, job);
                    return;
                }
                break;
        }
        jobNodeService.update(jobStep);
    }

    private void updateFinishStatus(JobStep jobStep, Cmd cmd, Job job){
        updateFlowStatus(jobStep, cmd, false, true);
        updateJobStatus(jobStep, cmd, false, true);
        deleteSession(job);
    }

    private void updateFlowStatus(JobStep jobStep, Cmd cmd, Boolean firstStep, Boolean lastStep) {
        JobFlow jobFlow = (JobFlow) nodeUtil.parentFlowNode(jobStep);
        jobFlow.setUpdatedAt(ZonedDateTime.now());
        jobFlow.setNodeStatus(jobStep.getNodeStatus());
        if(firstStep){
            jobFlow.setStartedTime(cmd.getCreatedDate());
        }

        if(lastStep){
            jobFlow.setFinishedAt(cmd.getFinishedDate());
        }
        //save
        jobNodeService.update(jobFlow);
    }

    private void updateJobStatus(JobStep jobStep, Cmd cmd,  Boolean firstStep, Boolean lastStep) {
        Job job = jobStep.getJob();
        job.setNodeStatus(jobStep.getNodeStatus());

        JobFlow jobFlow = (JobFlow) nodeUtil.parentFlowNode(jobStep);
        if(firstStep){
            jobFlow.setStartedTime(cmd.getCreatedDate());
        }

        if(lastStep){
            jobFlow.setFinishedAt(cmd.getFinishedDate());
        }
        //save
        mocJobList.put(job.getId(), job);
    }


    @Override
    public Job create(String flowName) {

        /**
         * TODO: copy JobFlow
         * TODO: copy jobStep
         * TODO: create job
         * TODO: create session
         */

        Job job = createJob(flowName);
        JobFlow jobFlow = copyJobFlow(flowName, job);
        copyJobStep(nodeUtil.allChildren(jobFlow), job);
        createSession(job);
        return null;
    }

    private Job createJob(String flowName){
        Job job = new Job();
        job.setCreatedAt(ZonedDateTime.now());
        job.setNodeStatus(NodeStatus.PENDING);
        job.setId(UUID.randomUUID().toString());
        mocJobList.put(job.getId(), job);
        return job;
    }

    private void copyJobStep(List<Node> nodes, Job job){
        nodes.forEach(item ->{
            Node newNode = ObjectUtil.deepCopy(item);
            JobStep jobStep = (JobStep)newNode;
            jobStep.setJob(job);
            jobNodeService.create(jobStep);
        });
    }

    private JobFlow copyJobFlow(String flowName, Job job){
        Node node = nodeService.find(flowName);
        Node newNode = ObjectUtil.deepCopy(node);
        JobFlow jobFlow = (JobFlow) newNode;
        jobFlow.setJob(job);
        jobNodeService.create(jobFlow);
        return jobFlow;
    }

    @Override
    public Job find(String id) {
        return mocJobList.get(id);
    }

    @Override
    public Boolean run(Node node) {
        if (node instanceof JobFlow) {
            runFlow(node);
        } else if (node instanceof JobStep) {
            runStep(node);
        } else {
            System.out.println("node is error");
        }
        return null;
    }

    @Override
    public Boolean handleCreateSessionCallBack(CmdBase cmdBase, String jobId) {
        if (cmdBase.getStatus() != CmdStatus.SENT){
            return null;
        }
        Job job = mocJobList.get(jobId);
        if(job == null)
            throw new RuntimeException("Not found job");
        //update job status
        job.setNodeStatus(NodeStatus.RUNNING);
        job.setUpdatedAt(ZonedDateTime.now());
        job.setSessionId(cmdBase.getSessionId());
        // save job status
        mocJobList.put(job.getId(), job);

        // find jobNode
        JobNode node = jobNodeService.find(job.getNodePath());
        node.setSessionId(job.getSessionId());
        // start run node
        run(node);
        return null;
    }

    @Override
    public Boolean createSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobWebhook(job));
        rabbitmqService.publish(routeKey, cmdInfo.toBytes());
        return null;
    }

    private void runStep(Node node) {
        JobNode jobNode = (JobNode) node;
        if (jobNode.getFinishedAt() != null) {
            // run next node
            run(nodeUtil.nextNodeFromAllChildren(node));
        } else {
            // POST to Agent to run shell
            CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.RUN_SHELL, node.getScript());
            cmdInfo.setWebhook(getStepWebhook((JobStep) node));
            cmdInfo.setSessionId(jobNode.getJob().getSessionId());
            new Thread(new RestClient(HttpMethod.POST, cmdInfo.toJson(), platform_url)).start();
        }
    }

    private void runFlow(Node node) {
        JobNode jobNode = (JobNode) node;

        //detect this node has run or not
        if (jobNode.getFinishedAt() != null) {
            // run next flow
            run(jobNode.getNext());
        } else {
            // not run
            List<Node> nodes = nodeUtil.allChildren(node);
            run(nodes.get(0));
        }
    }


    private String getStepWebhook(JobStep jobStep){
        return domain + "callback/" + jobStep.getPath() + "/message";
    }

    private String getJobWebhook(Job job){
        return domain + "callback/" + job.getId() + "/createSession";
    }

    private void deleteSession(Job job){
        CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());
        new Thread(new RestClient(HttpMethod.POST, cmdInfo.toJson(), platform_url)).start();
    }
}
