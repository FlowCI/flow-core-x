package com.flow.platform.cc.test;

import com.flow.platform.cc.service.ZkService;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by gy@fir.im on 18/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgentControllerTest extends TestBase {

    @Autowired
    private ZkService zkService;

    @Autowired
    private WebApplicationContext webAppContext;

    private MockMvc mockMvc;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    @Test
    public void should_list_all_online_agent() throws Exception {
        // given:
        String zoneName = "test-zone-01";
        zkService.createZone(zoneName);
        Thread.sleep(1000);

        String agentName = "act-001";
        ZkPathBuilder builder = ZkPathBuilder.create("flow-agents").append(zoneName).append(agentName);
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
        List<String> agentList = (List<String>) gson.fromJson(json, List.class);
        Assert.assertEquals(1, agentList.size());
        Assert.assertEquals(agentName, agentList.get(0));
    }
}
