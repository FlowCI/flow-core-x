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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.core.response.ResponseError;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.PathUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
public class FlowControllerTest extends TestBase {

    private final String flowName = "flow_default";

    @Before
    public void initToCreateEmptyFlow() throws Throwable {
        MockHttpServletRequestBuilder request = post("/flows/" + flowName)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Node flowNode = Node.parse(result.getResponse().getContentAsString(), Node.class);
        Assert.assertNotNull(flowNode);
        Assert.assertNotNull(flowNode.getEnv(GitEnvs.FLOW_GIT_WEBHOOK));
        Assert.assertEquals("PENDING", flowNode.getEnv(FlowEnvs.FLOW_STATUS));
    }

    @Test
    public void should_get_flow_node_detail() throws Throwable {
        MvcResult result = mockMvc.perform(get("/flows/" + flowName + "/show"))
            .andExpect(status().isOk())
            .andReturn();

        Node flowNode = Node.parse(result.getResponse().getContentAsString(), Node.class);
        Assert.assertNotNull(flowNode);
        Assert.assertEquals(flowName, flowNode.getName());
    }

    @Test
    public void should_get_env_value() throws Throwable {
        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/env")
            .param("key", "FLOW_STATUS")
            .param("editable", "false");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Assert.assertEquals("{\"FLOW_STATUS\":\"PENDING\"}", result.getResponse().getContentAsString());
    }

    @Test
    public void should_return_true_if_flow_name_exist() throws Throwable {
        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/exist")
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();
        BooleanValue existed = BooleanValue.parse(body, BooleanValue.class);
        Assert.assertNotNull(existed);
        Assert.assertEquals(true, existed.getValue());
    }

    @Test
    public void should_response_4xx_if_flow_name_format_invalid() throws Throwable {
        String flowName = "hello*gmail";

        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/exist")
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = this.mockMvc.perform(request)
            .andExpect(status().is4xxClientError())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        ResponseError error = ResponseError.parse(body, ResponseError.class);
        Assert.assertNotNull(error);
        Assert.assertEquals(error.getMessage(), "Illegal node name: hello*gmail");
    }

    @Test
    public void should_response_false_if_flow_name_not_exist() throws Throwable {
        // given:
        String flowName = "default";

        // when:
        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/exist")
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = this.mockMvc.perform(request)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String response = mvcResult.getResponse().getContentAsString();
        BooleanValue existed = BooleanValue.parse(response, BooleanValue.class);
        Assert.assertNotNull(existed);
        Assert.assertEquals(false, existed.getValue());
    }

    @Test
    public void should_response_4xx_if_env_not_defined_for_load_file_content() throws Throwable {
        // when: send request to load content
        MvcResult result = this.mockMvc.perform(get("/flows/" + flowName + "/yml/load"))
            .andExpect(status().is4xxClientError())
            .andReturn();

        // then:
        String response = result.getResponse().getContentAsString();
        ResponseError error = ResponseError.parse(response, ResponseError.class);
        Assert.assertTrue(error.getMessage().contains("Missing git settings"));
    }

    @Test
    public void should_get_yml_file_content() throws Throwable {
        // given:
        String yml = "flow:\n" + "  - name: " + flowName;
        Node flow = nodeService.find(flowName).root();
        setFlowToReady(flow);
        nodeService.createOrUpdateYml(PathUtil.build(flowName), yml);

        // when:
        MvcResult result = mockMvc.perform(get("/flows/" + flowName + "/yml"))
            .andExpect(status().isOk())
            .andReturn();
        String content = result.getResponse().getContentAsString();

        // then:
        Assert.assertEquals(yml, content);
    }

    @Test
    public void should_return_4xx_if_no_yml_content() throws Throwable {
        // when:
        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/yml");
        MvcResult result = mockMvc.perform(request).andExpect(status().is4xxClientError()).andReturn();
        String content = result.getResponse().getContentAsString();

        // then:
        ResponseError error = ResponseError.parse(content, ResponseError.class);
        Assert.assertEquals("Yml content not found", error.getMessage());
    }
}
