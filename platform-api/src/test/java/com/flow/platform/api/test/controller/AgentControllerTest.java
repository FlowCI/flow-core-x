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

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentPathWithPassword;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class AgentControllerTest extends TestBase {

    @Before
    public void before() {
        stubDemo();
    }

    @Test
    public void should_shutdown_success() throws Throwable {
        MockHttpServletRequestBuilder request = post("/agents/shutdown")
            .content(new AgentPathWithPassword("default", "machine", "123456").toJson())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        String contentAsString = result.getResponse().getContentAsString();

        BooleanValue booleanValue = BooleanValue.parse(contentAsString, BooleanValue.class);
        Assert.assertEquals(true, booleanValue.getValue());

        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(countStrategy, postRequestedFor(urlEqualTo("/cmd/send")));
    }

    @Test
    public void should_close_agent_success() throws Throwable {
        MvcResult result = mockMvc.perform(post("/agents/close")
            .content(new AgentPath("default", "machine").toJson())
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();


        String contentAsString = result.getResponse().getContentAsString();
        BooleanValue booleanValue = BooleanValue.parse(contentAsString, BooleanValue.class);
        Assert.assertEquals(true, booleanValue.getValue());

        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(countStrategy, postRequestedFor(urlEqualTo("/cmd/send")));
    }
}
