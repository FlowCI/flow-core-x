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

package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.*;
import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgentControllerTest extends TestBase {

    private final static String MOCK_CLOUD_PROVIDER_NAME = "test";

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Test
    public void should_list_all_online_agent() throws Throwable {
        // given:
        String zoneName = "test-zone-01";
        zoneService.createZone(new Zone(zoneName, MOCK_CLOUD_PROVIDER_NAME));
        Thread.sleep(1000);

        String agentName = "act-001";
        String path = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(path, null);

        Thread.sleep(1000);
        // when: send get request
        MvcResult result = this.mockMvc.perform(get("/agent/list").param("zone", zoneName))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String json = result.getResponse().getContentAsString();
        Assert.assertNotNull(json);

        Agent[] agentList = gsonConfig.fromJson(json, Agent[].class);
        Assert.assertEquals(1, agentList.length);
        Assert.assertEquals(agentName, agentList[0].getName());
    }

    @Test
    public void should_report_agent_status() throws Throwable {
        // given:
        String zoneName = "test-zone-03";
        zoneService.createZone(new Zone(zoneName, MOCK_CLOUD_PROVIDER_NAME));
        Thread.sleep(1000);

        String agentName = "act-003";
        String path = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(path, null);

        Thread.sleep(1000);
        // when: send agent info
        Agent agentObj = new Agent(zoneName, agentName);
        agentObj.setStatus(AgentStatus.BUSY);

        MockHttpServletRequestBuilder content = post("/agent/report")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gsonConfig.toJson(agentObj));

        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk());

        // then: check status from agent service
        Agent loaded = agentService.find(agentObj.getPath());
        Assert.assertEquals(AgentStatus.BUSY, loaded.getStatus());
    }

    @Test
    public void should_get_agent_token_success() throws Exception {
        AgentPath agentPath = new AgentPath("default", "test");
        MockHttpServletRequestBuilder content = post("/agent/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gsonConfig.toJson(agentPath));

        MvcResult result = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        Assert.assertNotNull(json);
    }

    @Test
    public void should_get_agent_info_success() throws Exception {
        AgentPath agentPath = new AgentPath("default", "test");
        MockHttpServletRequestBuilder content = post("/agent/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gsonConfig.toJson(agentPath));

        MvcResult result = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String token = result.getResponse().getContentAsString();

        result = this.mockMvc.perform(get("/agent/info").param("token", token))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String info = result.getResponse().getContentAsString();

        AgentSettings agentSettings = Jsonable.GSON_CONFIG.fromJson(info, AgentSettings.class);
        Assert.assertNotNull(agentSettings);
        Assert.assertEquals("default", agentSettings.getAgentPath().getZone());
        Assert.assertEquals("test", agentSettings.getAgentPath().getName());
    }


    @Test
    public void should_get_agent_info_fail() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get("/agent/info").param("token", "xxxxxxx"))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Assert.assertEquals("", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void should_get_agent_token_fail() throws Exception {
        AgentPath agentPath = new AgentPath("", "test");
        MockHttpServletRequestBuilder content = post("/agent/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gsonConfig.toJson(agentPath));

        MvcResult result = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().)
            .andReturn();
    }

}
