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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.security.AuthenticationInterceptor;
import com.flow.platform.api.test.TestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author yang
 */
public abstract class ControllerTestWithoutAuth extends TestBase {

    @Autowired
    private AuthenticationInterceptor authInterceptor;

    @Before
    public void disableAuth() {
        authInterceptor.disable();
    }

    @After
    public void enableAuth() {
        authInterceptor.enable();
    }

    void createEmptyFlow(String flow) throws Throwable {
        MvcResult result = mockMvc.perform(post("/flows/" + flow)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        Node flowNode = Node.parse(result.getResponse().getContentAsString(), Node.class);
        Assert.assertNotNull(flowNode);
        Assert.assertNotNull(flowNode.getEnv(GitEnvs.FLOW_GIT_WEBHOOK));
        Assert.assertEquals("PENDING", flowNode.getEnv(FlowEnvs.FLOW_STATUS));
    }

    void createYml(String flow, String resourcePath) throws Exception {
        String content = getResourceContent(resourcePath);

        mockMvc.perform(post("/flows/" + flow + "/yml").content(content))
            .andExpect(status().isOk())
            .andReturn();
    }
}
