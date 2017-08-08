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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeResultService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.test.util.NodeUtilYmlTest;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase {

    @Autowired
    private JobService jobService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeResultService jobNodeService;

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Test
    public void should_create_node_success() throws IOException {
        stubDemo();

        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource("demo_flow2.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, Charset.forName("UTF-8"));
        YmlStorage storage = new YmlStorage("/flow1", ymlString);
        ymlStorageDao.save(storage);
        Flow flow = (Flow) NodeUtil.buildFromYml(path);
        nodeService.create(flow);

        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");
        Step step3 = (Step) nodeService.find("/flow1/step3");

        Job job = jobService.createJob(flow.getPath());
        Assert.assertNotNull(job.getId());
        Assert.assertEquals(NodeStatus.ENQUEUE, job.getStatus());
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setSessionId("11111111");
        cmd.setStatus(CmdStatus.SENT);
        jobService.callback(job.getId().toString(), cmd);

        job = jobService.find(job.getId());
        Assert.assertEquals("11111111", job.getSessionId());

        cmd.setStatus(CmdStatus.RUNNING);
        cmd.setType(CmdType.RUN_SHELL);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        jobService.callback(Jsonable.GSON_CONFIG.toJson(map), cmd);
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, job.getStatus());
        job = jobService.find(job.getId());
        NodeResult jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.RUNNING, jobFlow.getStatus());

        cmd.setStatus(CmdStatus.LOGGED);
        cmd.setType(CmdType.RUN_SHELL);
        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(1);
        cmd.setCmdResult(cmdResult);

        map.put("path", step2.getPath());

        jobService.callback(Jsonable.GSON_CONFIG.toJson(map), cmd);
        job = jobService.find(job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, (jobNodeService.find(step2.getPath(), job.getId())).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, job.getStatus());
        jobFlow =  jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(NodeStatus.FAILURE, jobFlow.getStatus());

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

        nodeService.create(flow);
//        Job job = jobService.createJob(flow.getPath());
//        List<JobStep> jobSteps = jobService.listJobStep(job.getId());
//        Assert.assertEquals(2, jobSteps.size());
    }

}
