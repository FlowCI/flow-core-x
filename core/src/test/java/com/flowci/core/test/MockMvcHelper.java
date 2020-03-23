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

package com.flowci.core.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * @author yang
 */
public class MockMvcHelper {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    public String expectSuccessAndReturnString(RequestBuilder builder) throws Exception {
        MvcResult mvcResult = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return mvcResult.getResponse().getContentAsString();
    }

    public <T> T expectSuccessAndReturnClass(RequestBuilder builder, Class<T> clazz) throws Exception {
        MvcResult mvcResult = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        String json = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(json, clazz);
    }

    public <T> T expectSuccessAndReturnClass(RequestBuilder builder, TypeReference<T> typeRef) throws Exception {
        MvcResult mvcResult = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        String json = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(json, typeRef);
    }
}
