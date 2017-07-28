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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Cmd;
import java.util.UUID;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class JobControllerTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobService jobService;


    private void stubDemo() {
        Cmd cmdRes = new Cmd();
        cmdRes.setId(UUID.randomUUID().toString());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse()
                .withBody(cmdRes.toJson())));
    }


    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();

        Flow flow = new Flow();
        flow.setPath("/flow");
        flow.setName("flow");
        Step step1 = new Step();
        step1.setPath("/flow/step1");
        step1.setName("step1");
        Step step2 = new Step();
        step2.setPath("/flow/step2");
        step2.setName("step2");

        flow.getChildren().add(step1);
        flow.getChildren().add(step2);

        step1.setParent(flow);
        step2.setParent(flow);

        nodeService.create(flow);

        Job job = jobService.createJob(flow.getPath());

        MockHttpServletRequestBuilder content = get(new StringBuffer("/jobs/").append(job.getId()).toString())
            .contentType(MediaType.APPLICATION_JSON);
        ResultHandler resultHandler;
        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
//        String s = response.getContentAsString();
//        Job job1 = Jsonable.parse(s, Job.class);
//        Assert.assertEquals(job1 .getId(), job.getId());
    }

}
