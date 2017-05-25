package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Agent;
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

    @Autowired
    private WebApplicationContext webAppContext;

    private MockMvc mockMvc;

    private Gson gson = new Gson();

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
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
    public void should_send_cmd_to_agent() throws Throwable {
        // given:
        String zoneName = "test-zone-02";
        zkService.createZone(zoneName);
        Thread.sleep(1000);

        String agentName = "act-002";
        ZkPathBuilder builder = zkService.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        Thread.sleep(1000);

        // when: send post request
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "~/hello.sh");
        gson.toJson(cmd);

        MockHttpServletRequestBuilder content = post("/agent/cmd")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(cmd));

        // then: check response data
        MvcResult result = this.mockMvc.perform(content)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        Cmd cmdInfo = gson.fromJson(result.getResponse().getContentAsString(), Cmd.class);
        Assert.assertNotNull(cmdInfo);
        Assert.assertEquals(Cmd.Status.PENDING, cmdInfo.getStatus());
        Assert.assertEquals(zoneName, cmdInfo.getZone());
        Assert.assertEquals(agentName, cmdInfo.getAgent());

        // then: check node data
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, builder.path(), null);
        Assert.assertNotNull(raw);

        Cmd received = Cmd.parse(raw);
        Assert.assertNotNull(received);
        Assert.assertEquals(cmdInfo, received);
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
