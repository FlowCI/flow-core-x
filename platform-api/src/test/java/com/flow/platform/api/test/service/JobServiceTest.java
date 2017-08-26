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

import com.flow.platform.api.domain.CmdQueueItem;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase {

    @Test(expected = IllegalStatusException.class)
    public void should_raise_exception_since_flow_status_is_not_ready() throws IOException {
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        jobService.createJob(rootForFlow.getPath());
    }

    @Test
    public void should_create_node_success() throws IOException {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        Assert.assertNotNull(job.getId());
        Assert.assertEquals(NodeStatus.PENDING, job.getResult().getStatus());
        Step step1 = (Step) nodeService.find("flow1/step1");
        Step step2 = (Step) nodeService.find("flow1/step2");
        Step step3 = (Step) nodeService.find("flow1/step3");
        Flow flow = (Flow) nodeService.find(job.getNodePath());
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(new CmdQueueItem(job.getId().toString(), cmd));

        job = jobService.find(job.getId());
        Assert.assertEquals("11111111", job.getSessionId());

        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        jobService.callback(new CmdQueueItem(Jsonable.GSON_CONFIG.toJson(map), cmd));
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, job.getResult().getStatus());
        job = jobService.find(job.getId());
        NodeResult jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setType(CmdType.RUN_SHELL);
        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(1);
        cmdResult.setDuration(10l);
        cmd.setCmdResult(cmdResult);

        map.put("path", step2.getPath());

        jobService.callback(new CmdQueueItem(Jsonable.GSON_CONFIG.toJson(map), cmd));
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, (jobNodeResultService.find(step2.getPath(), job.getId())).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, job.getResult().getStatus());
        jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, jobFlow.getStatus());

    }

    @Test
    public void should_stop_success() throws IOException {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "demo_flow2.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());
        Assert.assertEquals(true, jobService.stopJob(job.getNodeName(), job.getNumber()));
        job = jobService.find(job.getNodeName(), job.getNumber());
        Assert.assertEquals(NodeStatus.STOPPED, job.getResult().getStatus());
    }

    @Test
    public void should_show_list_success() {
        stubDemo();

        Flow flow = new Flow("/flow", "flow");

        Step step1 = new Step("/flow/step1", "step1");
        Step step2 = new Step("/flow/step2", "step2");

        step1.setPlugin("step1");
        step1.setAllowFailure(true);
        step2.setPlugin("step2");
        step2.setAllowFailure(true);

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setParent(step1);

        // TODO: write yml converter

//        nodeService.create(flow);
//        Job job = jobService.createJob(flow.getPath());
//        List<JobStep> jobSteps = jobService.listJobStep(job.getId());
//        Assert.assertEquals(2, jobSteps.size());
    }

}
