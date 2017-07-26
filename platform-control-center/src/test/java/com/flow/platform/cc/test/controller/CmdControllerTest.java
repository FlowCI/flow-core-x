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

import static junit.framework.TestCase.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdReport;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.Zone;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Sets;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.util.NestedServletException;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CmdControllerTest extends TestBase {

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private Queue<Path> cmdLoggingQueue;

    @Before
    public void before() {
        cmdLoggingQueue.clear();
    }

    @Test
    public void should_list_cmd_types() throws Throwable {
        // when:
        MvcResult result = this.mockMvc.perform(get("/cmd/types"))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String raw = result.getResponse().getContentAsString();
        CmdType[] types = gsonConfig.fromJson(raw, CmdType[].class);
        Assert.assertNotNull(types);
        Assert.assertTrue(types.length == 6);
    }

    @Test
    public void should_update_cmd_status() throws Throwable {
        // given:
        String zone = "test-mos-mac";
        String agent = "test-001";

        AgentPath path = new AgentPath(zone, agent);
        agentService.reportOnline(zone, Sets.newHashSet(agent));

        CmdInfo base = new CmdInfo(zone, agent, CmdType.STOP, null);
        Cmd cmd = cmdService.create(base);

        // when:
        CmdReport postData = new CmdReport(cmd.getId(), CmdStatus.EXECUTED, new CmdResult());

        MockHttpServletRequestBuilder content = post("/cmd/report")
            .contentType(MediaType.APPLICATION_JSON)
            .content(postData.toJson());

        this.mockMvc.perform(content).andDo(print()).andExpect(status().isOk());

        // then: wait queue processing and check status
        Thread.sleep(2000);
        Cmd loaded = cmdService.find(cmd.getId());
        Assert.assertNotNull(loaded);
        Assert.assertTrue(loaded.getStatus().equals(CmdStatus.EXECUTED));
    }

    @Test
    public void should_send_cmd_to_agent() throws Throwable {
        // given:
        String zoneName = "test-zone-02";
        zoneService.createZone(new Zone(zoneName, "mock-cloud-provider-name"));
        Thread.sleep(1000);

        String agentName = "act-002";
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        Thread.sleep(1000);

        // when: send post request
        CmdInfo cmd = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, "~/hello.sh");
        cmd.getInputs().put("FLOW_P_1", "flow-1");
        cmd.getInputs().put("FLOW_P_2", "flow-2");
        cmd.setWorkingDir("/user/flow");

        gsonConfig.toJson(cmd);

        MockHttpServletRequestBuilder content = post("/cmd/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gsonConfig.toJson(cmd));

        // then: check response data
        MvcResult result = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        Cmd cmdInfo = gsonConfig.fromJson(result.getResponse().getContentAsString(), Cmd.class);
        Assert.assertNotNull(cmdInfo);
        Assert.assertTrue(cmdInfo.getStatus().equals(CmdStatus.SENT));
        Assert.assertEquals(zoneName, cmdInfo.getZoneName());
        Assert.assertEquals(agentName, cmdInfo.getAgentName());
        Assert.assertEquals(agentName, agentService.find(cmdInfo.getAgentPath()).getName());
        Assert.assertEquals(2, cmdInfo.getInputs().size());
        Assert.assertEquals("/user/flow", cmdInfo.getWorkingDir());

        // then: check node data
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, builder.path(), null);
        Assert.assertNotNull(raw);

        Cmd received = Jsonable.parse(raw, Cmd.class);
        Assert.assertNotNull(received);
        Assert.assertEquals(cmdInfo, received);
        Assert.assertEquals(2, received.getInputs().size());
        Assert.assertEquals("/user/flow", received.getWorkingDir());
    }

    @Test
    public void should_upload_and_download_zipped_log() throws Throwable {
        // given:
        ClassLoader classLoader = CmdControllerTest.class.getClassLoader();
        URL resource = classLoader.getResource("test-cmd-id.out.zip");
        Path path = Paths.get(resource.getFile());
        byte[] data = Files.readAllBytes(path);

        CmdInfo cmdBase = new CmdInfo("test-zone-1", "test-agent-1", CmdType.RUN_SHELL, "~/hello.sh");
        Cmd cmd = cmdService.create(cmdBase);

        String originalFilename = cmd.getId() + ".out.zip";

        MockMultipartFile zippedCmdLogPart = new MockMultipartFile("file", originalFilename, "application/zip", data);
        MockMultipartFile cmdIdPart = new MockMultipartFile("cmdId", "", "text/plain", cmd.getId().getBytes());

        // when: upload zipped cmd log
        MockMultipartHttpServletRequestBuilder content = fileUpload("/cmd/log/upload")
            .file(zippedCmdLogPart)
            .file(cmdIdPart);

        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk());

        // then: check upload file path and logging queue
        Path zippedLogPath = Paths.get(AppConfig.CMD_LOG_DIR.toString(), originalFilename);
        Assert.assertTrue(Files.exists(zippedLogPath));

        Assert.assertEquals(1, cmdLoggingQueue.size());
        Assert.assertEquals(zippedLogPath, cmdLoggingQueue.peek());
        Assert.assertEquals(data.length, Files.size(zippedLogPath));

        // when: download uploaded zipped cmd log
        MvcResult result = this.mockMvc.perform(get("/cmd/log/download")
            .param("cmdId", cmd.getId()).param("index", Integer.toString(0)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        MockHttpServletResponse response = result.getResponse();
        Assert.assertEquals("application/zip", response.getContentType());
        Assert.assertEquals(data.length, response.getContentLength());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains(originalFilename));
    }

    @Test
    public void should_raise_exception_if_illegal_parameter_for_queue() {
        // given:
        CmdInfo cmdBase = new CmdInfo("test-zone-1", "test-agent-1", CmdType.RUN_SHELL, "~/hello.sh");
        Assert.assertNotNull(cmdBase.getStatus());

        // when: request api of send cmd via queue with illegal priority
        MockHttpServletRequestBuilder content = post("/cmd/queue/send")
            .param("priority", "0")
            .param("retry", "0")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());

        // then: return 500 for illegal priority range
        try {
            this.mockMvc.perform(content).andExpect(status().is5xxServerError());
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(NestedServletException.class, e.getClass());
            Assert.assertEquals(IllegalParameterException.class, e.getCause().getClass());
        }

        // when: request api of send cmd via queue with illegal retry
        content = post("/cmd/queue/send")
            .param("priority", "1")
            .param("retry", "101")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cmdBase.toJson());

        // then: return 500 for illegal retry range
        try {
            this.mockMvc.perform(content).andExpect(status().is5xxServerError());
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(NestedServletException.class, e.getClass());
            Assert.assertEquals(IllegalParameterException.class, e.getCause().getClass());
        }
    }
}
