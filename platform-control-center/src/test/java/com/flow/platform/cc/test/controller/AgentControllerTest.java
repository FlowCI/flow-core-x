package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgentControllerTest extends TestBase {

    @Autowired
    private AgentService agentService;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Test
    public void should_has_agent_config_in_zone_data() throws Throwable {
        // given:
        String zoneName = "test-zone-00";
        zkService.createZone(zoneName);
        Thread.sleep(1000);

        // when:
        String zonePath = zkService.buildZkPath(zoneName, null).path();
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, zonePath, null);

        // then:
        AgentConfig config = AgentConfig.parse(raw);
        Assert.assertNotNull(config);
        Assert.assertEquals(socketIoUrl, config.getLoggingUrl());
        Assert.assertEquals(cmdReportUrl, config.getStatusUrl());
    }

    @Test
    public void should_list_all_online_agent() throws Throwable {
        // given:
        String zoneName = "test-zone-01";
        zkService.createZone(zoneName);
        Thread.sleep(1000);

        String agentName = "act-001";
        ZkPathBuilder builder = zkService.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

        // when: send get request
        MvcResult result = this.mockMvc.perform(get("/agent/list").param("zone", zoneName))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // then:
        String json = result.getResponse().getContentAsString();
        Assert.assertNotNull(json);

        Gson gson = new Gson();
        Agent[] agentList = gson.fromJson(json, Agent[].class);
        Assert.assertEquals(1, agentList.length);
        Assert.assertEquals(agentName, agentList[0].getName());
    }

    @Test
    public void should_report_agent_status() throws Throwable {
        // given:
        String zoneName = "test-zone-03";
        zkService.createZone(zoneName);
        Thread.sleep(1000);

        String agentName = "act-003";
        ZkPathBuilder builder = zkService.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

        // when: send agent info
        Agent agentObj = new Agent(zoneName, agentName);
        agentObj.setStatus(Agent.Status.BUSY);

        MockHttpServletRequestBuilder content = post("/agent/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(agentObj));

        this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk());

        // then: check status from agent service
        Agent loaded = agentService.find(agentObj.getPath());
        Assert.assertEquals(Agent.Status.BUSY, loaded.getStatus());
    }
}
