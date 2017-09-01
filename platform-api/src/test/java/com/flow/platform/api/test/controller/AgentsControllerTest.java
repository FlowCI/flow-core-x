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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class AgentsControllerTest extends TestBase {

    @Test
    public void should_shutdown_success() throws Exception{
        stubDemo();
        MockHttpServletRequestBuilder request = post("/agents/shutdown")
            .param("zone", "default")
            .param("name", "machine")
            .param("password", "123456");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        String contentAsString = result.getResponse().getContentAsString();

        BooleanValue booleanValue = BooleanValue.parse(contentAsString, BooleanValue.class);
        Assert.assertEquals(true, booleanValue.getValue());
    }
}
