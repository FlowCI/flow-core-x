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
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.job.JobNodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
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
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.MANUAL, null, mockUser);

        // then: check node result is created
        List<NodeResult> list = nodeResultService.list(job, false);
        Assert.assertEquals(5, list.size());

        NodeTree nodeTree = jobNodeService.get(job);

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
    public void should_correct_update_node_status_from_cmd() throws Throwable {
        // given: create job
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.MANUAL, null, mockUser);

        Node firstStep = jobNodeService.get(job).find("flow1/step1");

        // when: mock first step is logged
        nodeResultService.updateStatusByCmd(job, firstStep, createMockSuccessCmd(), null);

        // then:
        NodeResult firstStepResult = nodeResultService.find(firstStep.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, firstStepResult.getStatus());

        // when: mock first step send with running status after logged
        nodeResultService.updateStatusByCmd(job, firstStep, createMockRunningCmd(), null);

        // then: the node result should be SUCCESS as well
        firstStepResult = nodeResultService.find(firstStep.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.SUCCESS, firstStepResult.getStatus());
    }

    @Test
    public void should_update_node_status_with_skip_set() throws Throwable {
        // given:
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);

        List<NodeResult> list = nodeResultService.list(job, false);
        Assert.assertEquals(5, list.size());

        // when: set node result with diff status
        Node step11 = jobNodeService.get(job).find("flow1/step1/step11");
        nodeResultService.updateStatusByCmd(job, step11, createMockSuccessCmd(), null);

        Node step1 = jobNodeService.get(job).find("flow1/step1");
        nodeResultService.updateStatusByCmd(job, step1, createMockTimeOutCmd(), null);

        Node step2 = jobNodeService.get(job).find("flow1/step2");
        NodeResult resultForStep2 = nodeResultService.updateStatusByCmd(job, step2, createMockFailureCmd(), "Failure");
        Assert.assertEquals("Failure", resultForStep2.getFailureMessage());

        nodeResultService.updateStatus(job, STOPPED, Sets.newHashSet(SUCCESS, FAILURE, TIMEOUT));

        // then: check status of node result
        Assert.assertEquals(SUCCESS, nodeResultService.find("flow1/step1/step11", job.getId()).getStatus());
        Assert.assertEquals(STOPPED, nodeResultService.find("flow1/step1/step12", job.getId()).getStatus());
        Assert.assertEquals(TIMEOUT, nodeResultService.find("flow1/step1", job.getId()).getStatus());
        Assert.assertEquals(FAILURE, nodeResultService.find("flow1/step2", job.getId()).getStatus());
        Assert.assertEquals(STOPPED, nodeResultService.find("flow1", job.getId()).getStatus());
    }

    private Cmd createMockSuccessCmd() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setCmdResult(new CmdResult(0));
        return cmd;
    }

    private Cmd createMockRunningCmd() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setCmdResult(new CmdResult(null));
        return cmd;
    }

    private Cmd createMockTimeOutCmd() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);
        cmd.setCmdResult(new CmdResult(null));
        return cmd;
    }

    private Cmd createMockFailureCmd() {
        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.EXCEPTION);
        cmd.setCmdResult(new CmdResult(null));
        return cmd;
    }
}
