package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CmdControllerTest extends TestBase {

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

        MockHttpServletRequestBuilder content = post("/cmd/send")
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
}
