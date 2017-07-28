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
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectUtil;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author yh@firim
 */
@Service(value = "jobNodeService")
public class JobNodeServiceImpl implements JobNodeService {

    private final Map<String, JobNode> mocNodeList = new HashMap<>();
    private final Logger LOGGER = new Logger(JobService.class);

    @Autowired
    private NodeService nodeService;

    @Override
    public JobNode create(JobNode jobNode) {
        NodeUtil.recurse(jobNode, item -> {
            save((JobNode) item);
        });
        return jobNode;
    }

    @Override
    public JobNode save(JobNode jobNode) {
        mocNodeList.put(jobNode.getPath(), jobNode);
        return jobNode;
    }

    @Override
    public JobNode find(String nodePath) {
        return mocNodeList.get(nodePath);
    }


    /**
     * copy node to job node and save
     */
    @Override
    public JobNode createJobNode(String nodePath) {
        Node flow = nodeService.find(nodePath);
        NodeUtil.recurse(flow, node -> {
            JobNode jobNode;
            if (node instanceof Flow) {
                jobNode = copyNode(node, Flow.class, JobFlow.class);
            } else {
                jobNode = copyNode(node, Step.class, JobStep.class);
            }
            save(jobNode);

            // create job node
            for (int i = 0; i < node.getChildren().size(); i++) {
                Node child = (Node) node.getChildren().get(i);
                JobNode jobChild = find(child.getPath());
                jobNode.getChildren().add(jobChild);
                jobChild.setParent(jobNode);
                save(jobChild);
            }

            // build node prev next relation
            for (int i = 0; i < jobNode.getChildren().size(); i++) {
                JobNode jobChild = (JobNode) jobNode.getChildren().get(i);
                if (i == 0) {
                    // first node
                    jobChild.setPrev(null);
                    try {
                        jobChild.setNext((Node) jobNode.getChildren().get(i + 1));
                    } catch (Throwable ignore) {
                    }
                } else if (i == jobNode.getChildren().size() - 1) {
                    // last node
                    try {
                        jobChild.setPrev((Node) jobNode.getChildren().get(i - 1));
                    } catch (Throwable ignore) {
                    }
                    jobChild.setNext(null);
                } else {
                    // build node prev next relation
                    jobChild.setNext((Node) jobNode.getChildren().get(i + 1));
                    jobChild.setPrev((Node) jobNode.getChildren().get(i - 1));
                }
                save(jobChild);
            }
        });
        return (JobFlow) find(flow.getPath());
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
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.warn("copy node exception  %s - %s", e.getClass().toString(), e);
            throw new IllegalParameterException(
                String.format("Copy Node Exception Node - %s - %s", node.toJson(), e.toString()));
        }
        return (JobNode) k;
    }

}