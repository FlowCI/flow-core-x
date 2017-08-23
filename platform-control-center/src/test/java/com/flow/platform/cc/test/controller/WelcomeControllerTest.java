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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.cc.controller.WelcomeController;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.core.sysinfo.DBInfoLoader.DBGroupName;
import com.flow.platform.core.sysinfo.JvmLoader.JvmGroup;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.ZooKeeperLoader.ZooKeeperGroup;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author gy@fir.im
 */
public class WelcomeControllerTest extends TestBase {

    @Test
    public void should_get_ok_on_index() throws Throwable {
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
    }

    @Test
    public void should_load_system_jvm_info() throws Throwable {
        // when: load jvm info
        MvcResult result = this.mockMvc.perform(get("/sys/info/jvm"))
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String content = result.getResponse().getContentAsString();
        SystemInfo jvmInfo = SystemInfo.parse(content, SystemInfo.class);

        Map<String, String> jvmOsInfo = jvmInfo.get(JvmGroup.OS);
        Assert.assertNotNull(jvmOsInfo);
        Assert.assertEquals(6, jvmOsInfo.size());

        Map<String, String> jvmGeneralInfo = jvmInfo.get(JvmGroup.GENERAL);
        Assert.assertNotNull(jvmGeneralInfo);
        Assert.assertEquals(5, jvmGeneralInfo.size());

        Map<String, String> jvmMemoryInfo = jvmInfo.get(JvmGroup.MEMORY);
        Assert.assertNotNull(jvmMemoryInfo);
        Assert.assertEquals(3, jvmMemoryInfo.size());
    }

    @Test
    public void should_load_system_db_info() throws Throwable {
        // when: load db info
        MvcResult result = this.mockMvc.perform(get("/sys/info/db"))
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String content = result.getResponse().getContentAsString();
        SystemInfo dbInfo = SystemInfo.parse(content, SystemInfo.class);

        Map<String, String> mysqlInfo = dbInfo.get(DBGroupName.MYSQL);
        Assert.assertNotNull(mysqlInfo);
        Assert.assertEquals(5, mysqlInfo.size());
    }

    @Test
    public void should_load_zookeeper_info() throws Throwable {
        // when: load zookeeper info
        MvcResult result = this.mockMvc.perform(get("/sys/info/zk"))
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String content = result.getResponse().getContentAsString();
        SystemInfo zkInfo = SystemInfo.parse(content, SystemInfo.class);
        Assert.assertNotNull(zkInfo);
        Assert.assertEquals(SystemInfo.Status.RUNNING, zkInfo.getStatus());

        Map<String, String> conf = zkInfo.get(ZooKeeperGroup.CONF);
        Assert.assertEquals(8, conf.size());

        Map<String, String> srvr = zkInfo.get(ZooKeeperGroup.SRVR);
        Assert.assertEquals(8, srvr.size());
    }

    @Test
    public void should_load_rabbitmq_info() throws Throwable {
        // when: load zookeeper info
        MvcResult result = this.mockMvc.perform(get("/sys/info/mq"))
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String content = result.getResponse().getContentAsString();
        SystemInfo mqInfo = SystemInfo.parse(content, SystemInfo.class);
        Assert.assertNotNull(mqInfo);
    }
}