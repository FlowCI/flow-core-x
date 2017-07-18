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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public Boolean handleStatus(JobNode jobNode) {
        return null;
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
    public Boolean handleCreateSessionCallBack(Job job) {
        JobNode node = jobNodeService.find(job.getNodeName());
        // start run node
        run(node);
        return null;
    }

    @Override
    public Boolean createSession(Job job) {
        //TODO: rabbitmqService.publish
        return null;
    }

    private void runStep(Node node) {
        JobNode jobNode = (JobNode) node;
        if(jobNode.getFinishedAt() != null){
            // has run
            run(jobNode.getNext());
        }else{
            //TODO： notice agent to run shell
        }
    }

    private void runFlow(Node node) {
        JobNode jobNode = (JobNode) node;

        //detect this node has run or not
        if(jobNode.getFinishedAt() != null){
            // has run
            run(jobNode.getNext());
        }else{
            // not run
            List<Node> nodes = node.getChildren();
            run(findFirstNode(nodes));
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
}
