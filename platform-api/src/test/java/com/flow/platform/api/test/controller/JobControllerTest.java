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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.test.TestBase;

import com.flow.platform.api.domain.node.Node;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class JobControllerTest extends TestBase {

    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        Map<String, String> map = new HashMap<>();
        map.put("FLOW_GIT_BRANCH", "a");

        jobDao.update(job);

        StringBuilder stringBuilder = new StringBuilder("/jobs/");

        MockHttpServletRequestBuilder content = get(
            stringBuilder.append(job.getNodeName()).append("/").append(job.getNumber()).toString())
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        Job returnedJob = Job.parse(response, Job.class);

        // those fields cannot exported
        Assert.assertNull(returnedJob.getSessionId());
    }


    @Test
    public void should_stop_job_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        setFlowToReady(rootForFlow);
        Job job = jobService.createJob(rootForFlow.getPath());

        Map<String, String> map = new HashMap<>();
        map.put("FLOW_GIT_BRANCH", "a");

        jobDao.update(job);

        StringBuilder stringBuilder = new StringBuilder("/jobs/");

        MockHttpServletRequestBuilder content = post(
            stringBuilder.append(job.getNodeName()).append("/").append(job.getNumber()).append("/stop").toString())
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        BooleanValue t = BooleanValue.parse(response, BooleanValue.class);
        Assert.assertEquals(true, t.getValue());

        StringBuilder string = new StringBuilder("/jobs/");

        MockHttpServletRequestBuilder con = get(
            string.append(job.getNodeName()).append("/").append(job.getNumber()).toString())
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = this.mockMvc.perform(con)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String res = result.getResponse().getContentAsString();
        Job job1 = Job.parse(res, Job.class);
        Assert.assertEquals(NodeStatus.STOPPED, job1.getResult().getStatus());
    }
}
