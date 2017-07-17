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
 * @author gy@fir.im
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
        WelcomeController.AppStatus appStatus = gsonConfig.fromJson(content, WelcomeController.AppStatus.class);

        Assert.assertNotNull(appStatus);
        Assert.assertEquals("OK", appStatus.getStatus());
        Assert.assertEquals(ZkHelper.ZkStatus.OK, appStatus.getZkInfo().getStatus());
        Assert.assertEquals(1, appStatus.getZkHistory().get("flow-agents").size());
    }

}