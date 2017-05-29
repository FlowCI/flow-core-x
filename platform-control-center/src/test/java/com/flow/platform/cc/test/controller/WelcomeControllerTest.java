package com.flow.platform.cc.test.controller;

import com.flow.platform.cc.controller.WelcomeController;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZkHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

public class WelcomeControllerTest extends TestBase {

    @Test
    public void should_get_ok_on_index() throws Exception {
        // when:
        MvcResult result = this.mockMvc.perform(get("/index"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // then:
        String content = result.getResponse().getContentAsString();
        WelcomeController.AppStatus appStatus = gson.fromJson(content, WelcomeController.AppStatus.class);

        Assert.assertNotNull(appStatus);
        Assert.assertEquals("OK", appStatus.getStatus());
        Assert.assertEquals(ZkHelper.ZkStatus.OK, appStatus.getZkStatus());
        Assert.assertEquals(1, appStatus.getZkHistory().get("flow-agents").size());
    }

}