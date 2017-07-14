package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.*;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

    private final static String MOCK_CLOUD_PROVIDER_NAME = "test";

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Value("${agent.config.socket_io_url}")
    private String socketIoUrl;

    @Value("${agent.config.cmd_report_url}")
    private String cmdReportUrl;

    @Value("${agent.config.cmd_log_url}")
    private String cmdLogUrl;

    @Test
    public void should_has_agent_config_in_zone_data() throws Throwable {
        // given:
        String zoneName = "test-zone-00";
        zoneService.createZone(new Zone(zoneName, MOCK_CLOUD_PROVIDER_NAME));
        Thread.sleep(1000);

        // when:
        String zonePath = zkHelper.buildZkPath(zoneName, null).path();
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, zonePath, null);

        // then:
        AgentConfig config = Jsonable.parse(raw, AgentConfig.class);
        Assert.assertNotNull(config);
        Assert.assertEquals(socketIoUrl, config.getLoggingUrl());
        Assert.assertEquals(cmdReportUrl, config.getCmdStatusUrl());
        Assert.assertEquals(cmdLogUrl, config.getCmdLogUrl());
    }

    @Test
    public void should_list_all_online_agent() throws Throwable {
        // given:
        String zoneName = "test-zone-01";
        zoneService.createZone(new Zone(zoneName, MOCK_CLOUD_PROVIDER_NAME));
        Thread.sleep(1000);

        String agentName = "act-001";
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

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
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

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
}
