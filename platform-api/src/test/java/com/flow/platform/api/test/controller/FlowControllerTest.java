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

import com.flow.platform.api.response.ResponseError;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
public class FlowControllerTest extends TestBase {

    @Test
    public void should_response_4xx_if_flow_name_format_invalid() throws Throwable {
        String flowName = "hello*gmail";

        MockHttpServletRequestBuilder content = get("/flows/exist/" + flowName)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = this.mockMvc.perform(content)
            .andExpect(status().is4xxClientError())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        ResponseError error = ResponseError.parse(body, ResponseError.class);
        Assert.assertNotNull(error);
        Assert.assertEquals(error.getMessage(), "Invalid node name");
    }

    @Test
    public void should_response_true_if_flow_name_not_exist() throws Throwable {
        // given:
        String flowName = "default";

        // when:
        MockHttpServletRequestBuilder content = get("/flows/exist/" + flowName)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String response = mvcResult.getResponse().getContentAsString();
        Assert.assertNotNull(response);
        Assert.assertEquals(false, Boolean.parseBoolean(response));
    }
}
