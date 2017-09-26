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
package com.flow.platform.api.test.service;

import static com.flow.platform.api.domain.job.NodeStatus.FAILURE;
import static com.flow.platform.api.domain.job.NodeStatus.STOPPED;
import static com.flow.platform.api.domain.job.NodeStatus.SUCCESS;
import static com.flow.platform.api.domain.job.NodeStatus.TIMEOUT;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.job.JobNodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitEventType;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class NodeResultServiceTest extends TestBase {

    @Autowired
    private JobNodeService jobNodeService;

    @Before
    public void init() {
        stubDemo();

    }

    @Test
    public void should_save_job_node_by_job() throws IOException {
        // when: create node result list from job
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.MANUAL, null);

        // then: check node result is created
        List<NodeResult> list = nodeResultService.list(job, false);
        Assert.assertEquals(5, list.size());

        NodeTree nodeTree = jobNodeService.get(job.getId());

        // then: check cmd id is defined if the node is runnable
        for (NodeResult nodeResult : list) {
            if (nodeTree.canRun(nodeResult.getPath())) {
                Assert.assertEquals(String.format("%s-%s", job.getId(), nodeResult.getKey().getPath()),
                    nodeResult.getCmdId());
            }
        }

        // then: check flow node result is created
        NodeResult rootNodeResult = list.get(list.size() - 1);
        Assert.assertEquals(NodeTag.FLOW, rootNodeResult.getNodeTag());
    }

    @Test
    public void should_update_node_status_with_skip_set() throws Throwable {
        // given:
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createJob(rootForFlow.getPath(), GitEventType.TAG, null);
        List<NodeResult> list = nodeResultService.list(job, false);
        Assert.assertEquals(5, list.size());

        // when: set node result with diff status
        NodeResult resultForSuccess = list.get(0);
        resultForSuccess.setStatus(SUCCESS);
        nodeResultDao.update(resultForSuccess);

        NodeResult resultForTimeout = list.get(2);
        resultForTimeout.setStatus(TIMEOUT);
        nodeResultDao.update(resultForTimeout);

        NodeResult resultForFailure = list.get(3);
        resultForFailure.setStatus(FAILURE);
        nodeResultDao.update(resultForFailure);

        nodeResultService.updateStatus(job, STOPPED, Sets.newHashSet(SUCCESS, FAILURE, TIMEOUT));

        // then: check status of node result
        Assert.assertEquals(SUCCESS, nodeResultService.find("flow1/step1/step11", job.getId()).getStatus());
        Assert.assertEquals(STOPPED, nodeResultService.find("flow1/step1/step12", job.getId()).getStatus());
        Assert.assertEquals(TIMEOUT, nodeResultService.find("flow1/step1", job.getId()).getStatus());
        Assert.assertEquals(FAILURE, nodeResultService.find("flow1/step2", job.getId()).getStatus());
        Assert.assertEquals(STOPPED, nodeResultService.find("flow1", job.getId()).getStatus());
    }
}
