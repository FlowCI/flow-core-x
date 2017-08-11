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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class JobControllerTest extends TestBase {

    @Autowired
    private JobService jobService;

    @Autowired
    private NodeService nodeService;

    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();
        nodeService.createEmptyFlow("flow1");
        Job job = jobService.createJob(getResourceContent("flow.yaml"));

        MockHttpServletRequestBuilder content = get("/jobs/" + job.getId())
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        Job returnedJob = Job.parse(response, Job.class);
        Assert.assertEquals(returnedJob.getId(), job.getId());

        // those fields cannot exported
        Assert.assertNull(returnedJob.getExitCode());
        Assert.assertNull(returnedJob.getSessionId());
        Assert.assertNull(returnedJob.getCmdId());
    }
}
