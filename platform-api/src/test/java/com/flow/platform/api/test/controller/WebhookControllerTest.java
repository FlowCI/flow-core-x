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

package com.flow.platform.api.test.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.NodeResultService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.test.util.NodeUtilYmlTest;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.google.common.io.Files;
import com.google.gson.annotations.JsonAdapter;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class WebhookControllerTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeResultService jobNodeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private YmlStorageDao ymlStorageDao;

//    private void stubDemo() {
//        Cmd cmdRes = new Cmd();
//        cmdRes.setId(UUID.randomUUID().toString());
//        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/queue/send?priority=1&retry=5"))
//            .willReturn(aResponse()
//                .withBody(cmdRes.toJson())));
//
//        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/send"))
//            .willReturn(aResponse()
//                .withBody(cmdRes.toJson())));
//    }

    @Test
    public void should_callback_session_success() throws Exception {

        stubDemo();

        Flow flow = (Flow) initYaml("demo_flow.yaml");

        nodeService.create(flow);
        ;

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");

        // run first step running
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        cmdBase = cmd;
        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        NodeResult jobStep1 = jobNodeService.find(step1.getPath(), job.getId());
        NodeResult jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);

        // run first step finish
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        CmdResult cmdResult = new CmdResult();
        cmdResult.setExitValue(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        jobStep1 = jobNodeService.find(step1.getPath(), job.getId());
        jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep1.getExitCode());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);

        // run first step finish
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.LOGGED);
        cmdResult = new CmdResult();
        cmdResult.setExitValue(0);
        cmdResult.setDuration(100L);
        cmd.setCmdResult(cmdResult);

        cmdBase = cmd;
        map.put("path", step2.getPath());
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        NodeResult jobStep2 = jobNodeService.find(step2.getPath(), job.getId());
        jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(jobStep2.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep2.getExitCode());
        Assert.assertEquals(job.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.SUCCESS);
    }

    @Test
    public void should_callback_failure() throws Exception {

        stubDemo();

        Flow flow = (Flow) initYaml("demo_flow.yaml");

        nodeService.create(flow);
        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        job = jobService.find(job.getId());
        NodeResult jobStep1 = jobNodeService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(jobStep1.getCmdId());
        NodeResult jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        Assert.assertEquals(job.getStatus(), NodeStatus.FAILURE);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.TIMEOUT);
    }

    @Test
    public void should_callback_timeout_allow_failure() throws Exception {

        stubDemo();

        Flow flow = (Flow) initYaml("demo_flow1.yaml");

        nodeService.create(flow);

        Job job = jobService.createJob(flow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");

        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        cmdBase = cmd;
        content = post("/hooks?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        NodeResult jobStep1 = jobNodeService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(jobStep1.getCmdId());
        NodeResult jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        job = jobService.find(job.getId());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);
    }

    private Node initYaml(String fileName) throws IOException {
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, Charset.forName("UTF-8"));
        YmlStorage storage = new YmlStorage("/flow1", ymlString);
        ymlStorageDao.save(storage);

        return NodeUtil.buildFromYml(path);
    }
}
