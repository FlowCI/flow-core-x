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
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
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
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
public class JobServiceImpl implements JobService {

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

    @Value(value = "domain")
    private String domain;

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
        save(job);
        //create session
        createSession(job);
        return job;
    }

    private void createSession(Job job){
        CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(getJobHook(job));
        try {
            createSessionChannel.basicPublish(exchange, routeKey, null, cmdInfo.toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo("default", "default", CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());
        try {
            createSessionChannel.basicPublish(exchange, routeKey, null, cmdInfo.toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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

    @Override
    public void callback(String jobId, CmdBase cmdBase) {

    }

    @Override
    public void run(Node node) {

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

    private String getJobHook(Job job){
        return domain + "/callback/job/" + job.getId();
    }

    private String getNodeHook(Node node){
        return domain + "/callback/node/" + node.getPath();
    }
}
