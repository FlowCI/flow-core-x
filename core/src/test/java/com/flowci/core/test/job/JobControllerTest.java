/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.JsonablePage;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.job.domain.*;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.test.flow.FlowMockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author yang
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JobControllerTest extends MockLoggedInScenario {

    private static final TypeReference<ResponseMessage<Job>> JobType =
            new TypeReference<>() {
            };

    private static final TypeReference<ResponseMessage<JsonablePage<JobItem>>> JobListType =
            new TypeReference<>() {
            };

    private static final TypeReference<ResponseMessage<List<Step>>> JobStepsType =
            new TypeReference<>() {
            };

    private static final TypeReference<ResponseMessage<JobYml>> JobYmlType =
            new TypeReference<>() {
            };

    @Autowired
    private FlowMockHelper flowMockHelper;

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private final String flow = "hello-flow";

    @BeforeEach
    void init() throws Exception {
        String yml = StringHelper.toString(load("flow.yml"));
        flowMockHelper.create(flow, yml);
    }

    @Test
    void should_get_job_yml() throws Exception {
        createJobForFlow(flow);

        var responseMessage = mockMvcHelper.expectSuccessAndReturnClass(get("/jobs/hello-flow/1/yml"), JobYmlType);
        List<JobYml.Body> list = responseMessage.getData().getList();
        assertEquals(1, list.size());

        String yml = StringHelper.fromBase64(list.get(0).getRawInB64());
        assertNotNull(yml);
        assertEquals(StringHelper.toString(load("flow.yml")), yml);
    }

    @Test
    void should_get_job_by_name_and_build_number() throws Exception {
        // init: create job
        Job created = createJobForFlow(flow);
        assertNotNull(created);

        // when:
        ResponseMessage<Job> response = mockMvcHelper.expectSuccessAndReturnClass(get("/jobs/hello-flow/1"), JobType);
        assertEquals(StatusCode.OK, response.getCode());

        // then:
        Job loaded = response.getData();
        assertNotNull(loaded);
        assertEquals(created, loaded);
    }

    @Test
    void should_get_latest_job() throws Exception {
        // init:
        Job first = createJobForFlow(flow);
        assertEquals(1, first.getBuildNumber().intValue());

        Job second = createJobForFlow(flow);
        assertEquals(2, second.getBuildNumber().intValue());

        // when:
        ResponseMessage<Job> response =
                mockMvcHelper.expectSuccessAndReturnClass(get("/jobs/hello-flow/latest"), JobType);
        assertEquals(StatusCode.OK, response.getCode());

        // then:
        Job latest = response.getData();
        assertNotNull(latest);
        assertEquals(2, latest.getBuildNumber().intValue());
    }

    @Test
    void should_list_job_by_flow() throws Exception {
        // init:
        Job first = createJobForFlow(flow);
        Job second = createJobForFlow(flow);

        // when:
        ResponseMessage<JsonablePage<JobItem>> message = mockMvcHelper
                .expectSuccessAndReturnClass(get("/jobs/hello-flow"), JobListType);
        assertEquals(StatusCode.OK, message.getCode());

        // then:
        Page<JobItem> page = message.getData().toPage();
        assertEquals(2, page.getTotalElements());

        assertEquals(second.getId(), page.getContent().get(0).getId());
        assertEquals(first.getId(), page.getContent().get(1).getId());
    }

    @Test
    void should_list_job_steps_by_flow_and_build_number() throws Exception {
        // init:
        createJobForFlow(flow);

        // when:
        ResponseMessage<List<Step>> message = mockMvcHelper
                .expectSuccessAndReturnClass(get("/jobs/hello-flow/1/steps"), JobStepsType);
        assertEquals(StatusCode.OK, message.getCode());

        // then: should have 3 steps include root step
        List<Step> steps = message.getData();
        assertEquals(3, steps.size());
        for (Step s : steps) {
            assertEquals(Step.Status.PENDING, s.getStatus());
        }
    }

    public Job createJobForFlow(String name) throws Exception {
        return mockMvcHelper.expectSuccessAndReturnClass(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CreateJob(name))), JobType)
                .getData();
    }
}
