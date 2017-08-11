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
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeResultService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class CmdWebhookControllerTest extends TestBase {

    private final static String CMD_HOOKS_URL = "/hooks/cmd?identifier=";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobNodeResultService jobNodeService;

    @Autowired
    private JobService jobService;

    @Test
    public void should_callback_session_success() throws Exception {
        stubDemo();
        nodeService.createEmptyFlow("flow1");
        Job job = jobService.createJob(getResourceContent("demo_flow.yaml"));

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
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

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
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
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
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
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
        nodeService.createEmptyFlow("flow1");
        Job job = jobService.createJob(getResourceContent("demo_flow.yaml"));
        Step step2 = (Step) nodeService.find("/flow1/step2");
        Step step1 = (Step) nodeService.find("/flow1/step1");
        Flow flow = (Flow) nodeService.find(job.getNodePath());

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
        content = post("/hooks/cmd?identifier=" + UrlUtil.urlEncoder(Jsonable.GSON_CONFIG.toJson(map)))
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
        nodeService.createEmptyFlow("flow1");
        Job job = jobService.createJob(getResourceContent("demo_flow1.yaml"));

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
        job = jobService.find(job.getId());
        Assert.assertNotNull(job.getSessionId());
        Assert.assertNotNull(job.getCmdId());
        Assert.assertEquals(sessionId, job.getSessionId());
        Assert.assertEquals(job.getStatus(), NodeStatus.ENQUEUE);

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

        NodeResult jobStep1 = jobNodeService.find(step1.getPath(), job.getId());
        Assert.assertNotNull(jobStep1.getCmdId());
        NodeResult jobFlow = jobNodeService.find(flow.getPath(), job.getId());
        job = jobService.find(job.getId());
        Assert.assertEquals(job.getStatus(), NodeStatus.RUNNING);
        Assert.assertEquals(jobStep1.getStatus(), NodeStatus.TIMEOUT);
        Assert.assertEquals(jobFlow.getStatus(), NodeStatus.RUNNING);
    }
}
