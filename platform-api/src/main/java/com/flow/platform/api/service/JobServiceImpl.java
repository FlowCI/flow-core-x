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
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.util.RestClient;
import com.flow.platform.api.util.RestClient.HttpMethod;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdType;
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
    private RabbitmqService rabbitmqService;

    @Value(value = "${domain}")
    private String domain;

    @Value(value = "${rabbitmq.routeKey}")
    private String routeKey;

    @Value(value = "${platform.api")
    private String platform_url;

    @Override
    public Boolean handleCmdResult(Cmd cmd, JobNode jobNode) {
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
        Job job = findJobFlow(jobStep).getJob();
        switch (cmd.getStatus()) {
            case PENDING:
                nodeStatus = NodeStatus.PENDING;
                jobStep.setNodeStatus(nodeStatus);
                break;
            case RUNNING:
                nodeStatus = NodeStatus.RUNNING;
                jobStep.setNodeStatus(nodeStatus);
                //first step
                if(findPrevJobStep(jobStep) == null){
                    updateFlowStatus(jobStep, cmd, true, false);
                    updateJobStatus(jobStep, cmd, true, false);
                }

                break;
            case EXECUTED:
            case LOGGED:
                nodeStatus = NodeStatus.SUCCESS;
                jobStep.setNodeStatus(nodeStatus);
                JobStep next = findNextJobStep(jobStep);
                // end step
                if(next == null){
                    updateFinishStatus(jobStep, cmd, job);
                    return;
                }
                JobFlow jobFlow = findJobFlow(jobStep);
                run(next, job);
                break;
            case REJECTED:
            case KILLED:
            case EXCEPTION:
                nodeStatus = NodeStatus.FAIL;
                jobStep.setNodeStatus(nodeStatus);
                next = findNextJobStep(jobStep);
                // end step
                if(next == null){
                    updateFinishStatus(jobStep, cmd, job);
                    return;
                }
                break;
            case TIMEOUT_KILL:
                nodeStatus = NodeStatus.TIMEOUT;
                jobStep.setNodeStatus(nodeStatus);
                next = findNextJobStep(jobStep);
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
        JobFlow jobFlow = findJobFlow(jobStep);
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
        JobFlow jobFlow = findJobFlow(jobStep);
        Job job = jobFlow.getJob();
        job.setNodeStatus(jobStep.getNodeStatus());

        if(firstStep){
            jobFlow.setStartedTime(cmd.getCreatedDate());
        }

        if(lastStep){
            jobFlow.setFinishedAt(cmd.getFinishedDate());
        }
        //save
        mocJobList.put(job.getId(), job);
    }

    private JobFlow findJobFlow(JobStep jobStep) {
        JobFlow jobFlow;
        Node tmp = jobStep;
        Node node;
        while (true) {
            if (tmp == null) {
                throw new RuntimeException("no parent node");
            }
            node = tmp.getParent();
            if (node instanceof JobFlow) {
                jobFlow = (JobFlow) node;
                break;
            }
            tmp = node;
        }
        return jobFlow;
    }

    private JobStep findNextJobStep(JobStep jobStep) {
        Node tmp = jobStep;
        if (tmp.getNext() != null) {
            return findChildrenFirstJobStep((JobStep) tmp.getNext());
        }

        if (tmp.getParent() instanceof JobStep) {
            return (JobStep) tmp.getParent();
        }

        // has no next jobStep
        return null;
    }

    private JobStep findPrevJobStep(JobStep jobStep) {
        Node tmp = jobStep;
        if (tmp.getPrev() != null) {
            return (JobStep) tmp.getPrev();
        }

        if (!tmp.getChildren().isEmpty()) {
            return (JobStep) findChildrenLastJobStep((JobStep) tmp);
        }

        // has no prev jobStep
        if(tmp.getParent() instanceof JobFlow){
            System.out.println("has no parent prev node");
            return null;
        }

        findPrevJobStep((JobStep) jobStep.getParent());
        // has no next jobStep
        return null;
    }


    private JobStep findChildrenFirstJobStep(JobStep jobStep) {
        Node tmp = jobStep;
        Node firstNode = findFirstNode(tmp.getChildren());
        if (firstNode == null) {
            return jobStep;
        } else {
            return (JobStep) firstNode;
        }
    }

    private JobStep findChildrenLastJobStep(JobStep jobStep) {
        Node tmp = jobStep;
        Node lastNode = findLastNode(jobStep.getChildren());
        if (lastNode == null) {
            return jobStep;
        }else{
            return (JobStep)lastNode;
        }
    }

    @Override
    public Job create(Job job) {
        String id = UUID.randomUUID().toString();
        job.setId(id);
        job.setCreatedAt(new Date());
        job.setUpdatedAt(new Date());
        mocJobList.put(id, job);
        return job;
    }

    @Override
    public Job find(String id) {
        return mocJobList.get(id);
    }

    @Override
    public Boolean run(Node node, Job job) {
        if (node instanceof JobFlow) {
            runFlow(node, job);
        } else if (node instanceof JobStep) {
            runStep(node, job);
        } else {
            System.out.println("node is error");
        }
        return null;
    }

    @Override
    public Boolean handleCreateSessionCallBack(CmdBase cmdBase, Job job) {

        //update job status
        job.setNodeStatus(NodeStatus.RUNNING);
        job.setUpdatedAt(new Date());
        job.setSessionId(cmdBase.getSessionId());
        // save job status
        mocJobList.put(job.getId(), job);

        // find jobNode
        JobNode node = jobNodeService.find(job.getNodePath());
        node.setSessionId(job.getSessionId());
        // start run node
        run(node, job);
        return null;
    }

    @Override
    public Boolean createSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobWebhook(job));
        rabbitmqService.publish(routeKey, cmdInfo.toBytes());
        return null;
    }

    private void runStep(Node node, Job job) {
        JobNode jobNode = (JobNode) node;
        if (jobNode.getFinishedAt() != null) {
            // has run
            run(jobNode.getNext(), job);
        } else {
            // POST to Agent to run shell
            CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.RUN_SHELL, node.getScript());
            cmdInfo.setWebhook(getStepWebhook((JobStep) node));
            cmdInfo.setSessionId(job.getSessionId());
            new Thread(new RestClient(HttpMethod.POST, cmdInfo.toJson(), platform_url)).start();
        }
    }

    private void runFlow(Node node, Job job) {
        JobNode jobNode = (JobNode) node;

        //detect this node has run or not
        if (jobNode.getFinishedAt() != null) {
            // has run
            run(jobNode.getNext(), job);
        } else {
            // not run
            List<Node> nodes = node.getChildren();
            run(findFirstNode(nodes), job);
        }
    }

    private Node findFirstNode(List<Node> nodes) {
        Node node = nodes.get(0);
        if (node == null) {
            System.out.println("flow has no children");
            return null;
        }
        Node first = node;
        Node tmp = null;
        while (true) {
            tmp = first.getPrev();
            if (tmp == null) {
                break;
            }
            first = tmp;
        }
        return first;
    }

    private Node findLastNode(List<Node> nodes) {
        Node node = nodes.get(0);
        if (node == null) {
            System.out.println("flow has no children");
            return null;
        }
        Node last = node;
        Node tmp;
        while (true) {
            tmp = last.getNext();
            if (tmp == null) {
                break;
            }
            last = tmp;
        }
        return last;
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
