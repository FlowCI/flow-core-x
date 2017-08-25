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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.node.NodeStatus;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class CmdWebhookControllerTest extends TestBase {

    @Before
    public void before() throws Throwable {
        stubDemo();
    }

    @Test
    public void should_callback_session_success() throws Exception {
        Node rootForFlow = createRootFlow("flow1", "demo_flow.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        Thread.sleep(1000);
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.PENDING);

        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");
        Flow flow = (Flow) nodeService.find(job.getNodePath());
        // run first step running
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        cmdBase = cmd;
        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        Thread.sleep(1000);
        job = jobService.find(job.getId());
        NodeResult jobStep1 = jobNodeResultService.find(step1.getPath(), job.getId());
        NodeResult jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.RUNNING);
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
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        Thread.sleep(1000);
        job = jobService.find(job.getId());
        jobStep1 = jobNodeResultService.find(step1.getPath(), job.getId());
        jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep1.getExitCode());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.RUNNING);
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
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        Thread.sleep(1000);
        job = jobService.find(job.getId());
        NodeResult jobStep2 = jobNodeResultService.find(step2.getPath(), job.getId());
        jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        Assert.assertEquals(jobStep2.getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals((Integer) 0, jobStep2.getExitCode());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.SUCCESS);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.SUCCESS);
    }

    @Test
    public void should_callback_failure() throws Exception {
        // init
        Node rootForFlow = createRootFlow("flow1", "demo_flow.yaml");
        setFlowToReady(rootForFlow);

        Job job = jobService.createJob(rootForFlow.getPath());
        Step step2 = (Step) nodeService.find("/flow1/step2");
        Step step1 = (Step) nodeService.find("/flow1/step1");
        Flow flow = (Flow) nodeService.find(job.getNodePath());

        // create session
        CmdBase cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        MockHttpServletRequestBuilder content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(1000);

        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(NodeStatus.PENDING, job.getResult().getStatus());

        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmd.toJson());

        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(1000);

        job = jobService.find(job.getId());

        NodeResult jobStep1 = jobNodeResultService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(jobStep1.getCmdId());
        NodeResult jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());

        Assert.assertEquals(NodeStatus.TIMEOUT, job.getResult().getStatus());
        Assert.assertEquals(NodeStatus.TIMEOUT, jobStep1.getStatus());
        Assert.assertEquals(NodeStatus.TIMEOUT, jobFlow.getStatus());
    }

    @Test
    public void should_callback_timeout_allow_failure() throws Exception {
        Node rootForFlow = createRootFlow("flow1", "demo_flow1.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        // create session
        Cmd cmd = new Cmd("default", null, CmdType.CREATE_SESSION, null);
        cmd.setStatus(CmdStatus.SENT);
        String sessionId = "1111111";
        cmd.setSessionId(sessionId);

        CmdBase cmdBase = cmd;
        MockHttpServletRequestBuilder content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(job.getId().toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(1000);

        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.PENDING);

        Step step1 = (Step) nodeService.find("/flow1/step1");
        Step step2 = (Step) nodeService.find("/flow1/step2");
        Flow flow = (Flow) nodeService.find(job.getNodePath());
        // run first step timeout
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.RUNNING);

        Map<String, String> map = new HashMap<>();
        map.put("path", step1.getPath());
        map.put("jobId", job.getId().toString());

        cmdBase = cmd;
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Thread.sleep(1000);
        cmd = new Cmd("default", null, CmdType.RUN_SHELL, step1.getScript());
        cmd.setStatus(CmdStatus.TIMEOUT_KILL);

        cmdBase = cmd;
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());
        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        Thread.sleep(1000);
        NodeResult jobStep1 = jobNodeResultService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(jobStep1.getCmdId());
        NodeResult jobFlow = jobNodeResultService.find(flow.getPath(), job.getId());
        job = jobService.find(job.getId());
        Assert.assertEquals(job.getResult().getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);
    }
}
