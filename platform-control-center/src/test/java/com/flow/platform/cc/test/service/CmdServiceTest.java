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

package com.flow.platform.cc.test.service;

import static junit.framework.TestCase.fail;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.Zone;
import com.flow.platform.exception.IllegalParameterException;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.JVM)
public class CmdServiceTest extends TestBase {

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private List<Zone> defaultZones;

    private final static String MOCK_PROVIDER_NAME = "mock-cloud-provider";

    private Process mockProcess = new Process() {
        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public InputStream getErrorStream() {
            return null;
        }

        @Override
        public int waitFor() throws InterruptedException {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {

        }
    };

    @Test
    public void should_create_cmd() {
        // given:
        String zoneName = defaultZones.get(0).getName();
        CmdInfo base = new CmdInfo(zoneName, "test-agent", CmdType.KILL, null);
        base.setWebhook("http://hooks.com");

        // when:
        Cmd cmd = cmdService.create(base);
        Assert.assertNotNull(cmd);
        Assert.assertNotNull(cmd.getId());
        Assert.assertNotNull(cmd.getCreatedDate());
        Assert.assertNotNull(cmd.getUpdatedDate());
        Assert.assertNotNull(cmd.getWebhook());
        Assert.assertNotNull(cmd.getTimeout()); // should have default timeout from zone setting

        // then:
        Cmd loaded = cmdService.find(cmd.getId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(cmd.getId(), loaded.getId());
        Assert.assertEquals(base.getWebhook(), cmd.getWebhook());
    }

    @Test
    public void should_report_cmd_status() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        AgentPath agentPath = new AgentPath(zoneName, "test-agent-for-report-cmd");
        agentService.reportOnline(zoneName, Sets.newHashSet("test-agent-for-report-cmd"));
        Thread.sleep(5000);

        Agent agent = agentService.find(agentPath);
        Assert.assertNotNull(agent);

        CmdInfo base = new CmdInfo(agentPath, CmdType.RUN_SHELL, null);
        Cmd cmd = cmdService.create(base);
        Assert.assertNotNull(cmd);
        Assert.assertNotNull(cmd.getId());

        // when: create a mock result and update cmd status
        CmdResult result = new CmdResult();
        result.setStartTime(ZonedDateTime.now());
        result.setProcess(mockProcess);
        result.setProcessId(mockProcess.hashCode());

        result.getOutput().put("FLOW_API", "123");
        result.getOutput().put("FLOW_TEST", "456");

        result.getExceptions().add(new RuntimeException("Dummy exception 1"));
        result.getExceptions().add(new RuntimeException("Dummy exception 2"));

        CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.RUNNING, result, true, true);
        cmdService.updateStatus(statusItem, false);

        // then: check cmd status should be running and agent status should be busy
        Cmd loaded = cmdService.find(cmd.getId());
        Assert.assertTrue(loaded.getStatus().equals(CmdStatus.RUNNING));
        Assert.assertNotNull(cmdResultDao.get(cmd.getId()));
        Assert.assertEquals((Integer) mockProcess.hashCode(), cmdResultDao.get(cmd.getId()).getProcessId());
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentPath).getStatus());

        List<CmdResult> loadedResults = cmdService.listResult(Sets.newHashSet(cmd.getId()));
        Assert.assertEquals(1, loadedResults.size());
        Assert.assertEquals(2, loadedResults.get(0).getOutput().size());
        Assert.assertEquals(2, loadedResults.get(0).getExceptions().size());

        // when: update status for empty output and null process id
        result = new CmdResult();
        result.setProcessId(null);
        result.getOutput().clear();

        statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.RUNNING, result, true, true);
        cmdService.updateStatus(statusItem, false);

        // then:
        loadedResults = cmdService.listResult(Sets.newHashSet(cmd.getId()));
        Assert.assertEquals(1, loadedResults.size());
        Assert.assertNotNull(loadedResults.get(0).getProcessId());
        Assert.assertEquals(2, loadedResults.get(0).getOutput().size());
        Assert.assertEquals(2, loadedResults.get(0).getExceptions().size());
    }

    @Test
    public void should_cmd_timeout() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-for-timeout";

        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);
        Thread.sleep(500);

        // when: send cmd
        Cmd cmd = cmdService.send(new CmdInfo(zoneName, null, CmdType.RUN_SHELL, "test"));
        cmd.setTimeout(600);
        Assert.assertTrue(cmd.isCurrent());

        // then: check should not timeout
        Assert.assertEquals(false, cmdService.isTimeout(cmd));

        // when: mock cmd timeout
        ZonedDateTime timeoutDate = ZonedDateTime.now().minusSeconds(cmd.getTimeout() + 10);
        cmd.setCreatedDate(timeoutDate);
        cmd.setStatus(CmdStatus.RUNNING);
        cmdDao.update(cmd);

        // then: should timeout and status should be TIMEOUT_KILL
        Assert.assertEquals(true, cmdService.isTimeout(cmd));
        cmdService.checkTimeoutTask();
        Thread.sleep(500); // wait for cmd status update queue to process
        Assert.assertEquals(CmdStatus.TIMEOUT_KILL, cmdService.find(cmd.getId()).getStatus());
    }

    @Test
    public void should_update_agent_status_by_cmd_status() throws Throwable {
        // given
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-001";
        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);
        Thread.sleep(1000);

        // should Agent.status.IDLE from cmd finish status
        for (CmdStatus reportStatus : Cmd.FINISH_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdInfo base = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            CmdStatusItem statusItem = new CmdStatusItem(current.getId(), reportStatus, new CmdResult(), true, true);
            cmdService.updateStatus(statusItem, false);

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current.getId(), loaded.getId());

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.IDLE, relatedAgent.getStatus());
        }

        // Agent.status.BUSY from cmd working status
        for (CmdStatus status : Cmd.WORKING_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdInfo base = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            CmdStatusItem statusItem = new CmdStatusItem(current.getId(), status, new CmdResult(), true, true);
            cmdService.updateStatus(statusItem, false);

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current, loaded);

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            // reset agent status
            relatedAgent.setStatus(AgentStatus.IDLE);
            agentDao.update(relatedAgent);
        }
    }

    @Test
    public void should_send_cmd_to_agent() throws InterruptedException {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-002";

        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);
        Thread.sleep(1000);

        // when: send command
        CmdInfo cmd = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        Cmd cmdInfo = cmdService.send(cmd);

        // then:

        // check cmd status
        Assert.assertNotNull(cmdInfo.getId());
        Assert.assertTrue(cmdInfo.getStatus().equals(CmdStatus.SENT));
        Assert.assertEquals(zoneName, cmdInfo.getZoneName());
        Assert.assertEquals(agentName, cmdInfo.getAgentName());

        // check cmd been recorded
        Assert.assertTrue(cmdService.listByAgentPath(cmd.getAgentPath()).contains(cmdInfo));

        // check agent status
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(cmd.getAgentPath()).getStatus());

        // check zk node received the same cmd
        byte[] raw = zkClient.getData(agentPath);
        Cmd loaded = Jsonable.parse(raw, Cmd.class);
        Assert.assertEquals(cmdInfo, loaded);

        Thread.sleep(1000); // mock network delay

        // when: send command again to the same agent
        try {
            cmdService.send(cmd);
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }

        // then:
        List<Cmd> cmdList = cmdService.listByAgentPath(cmd.getAgentPath());
        Assert.assertEquals(2, cmdList.size());
        Assert.assertTrue(cmdList.get(1).getStatus().equals(CmdStatus.REJECTED));
    }

    @Test(expected = IllegalParameterException.class)
    public void should_throw_exception_when_shutdown_cmd_miss_password() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-for-shutdown";
        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);
        Thread.sleep(1000);

        // when: send shutdown command
        CmdInfo cmd = new CmdInfo(zoneName, agentName, CmdType.SHUTDOWN, null);
        cmdService.send(cmd);
    }

    @Test
    public void should_auto_select_idle_agent_when_cmd_send() throws Throwable {
        // given:
        String zoneName = "auto-select-zone";
        zoneService.createZone(new Zone(zoneName, MOCK_PROVIDER_NAME));
        Thread.sleep(1000);

        AgentPath agentIdle1 = new AgentPath(zoneName, "idle-agent-01");
        AgentPath agentIdle2 = new AgentPath(zoneName, "idle-agent-02");
        AgentPath agentBusy1 = new AgentPath(zoneName, "busy-agent-01");

        zkClient.createEphemeral(ZKHelper.buildPath(agentIdle1), null);
        zkClient.createEphemeral(ZKHelper.buildPath(agentIdle2), null);
        zkClient.createEphemeral(ZKHelper.buildPath(agentBusy1), null);
        Thread.sleep(2000);

        // report busy status
        cmdService.send(new CmdInfo(agentBusy1, CmdType.RUN_SHELL, "echo \"hello\""));
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentBusy1).getStatus());

        // set idle agent 1 date, before idle agent 2
        Agent agent = agentService.find(agentIdle2);
        agentDao.update(agent);

        // when: send cmd to zone

        Cmd cmdForIdle1 = cmdService.send(new CmdInfo(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));

        // then: should select agent idle 1 as target
        Assert.assertEquals(agentIdle1, cmdForIdle1.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentIdle1).getStatus());

        // when: send cmd to make all agent to busy

        Cmd cmdForIdle2 = cmdService.send(new CmdInfo(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));
        Assert.assertEquals(agentIdle2, cmdForIdle2.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentIdle2).getStatus());

        // then: should raise NotAvailableException
        try {
            cmdService.send(new CmdInfo(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_exception_agent_not_exit() {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-003";

        // then: send command immediately should raise AgentErr.NotFoundException
        CmdInfo cmd = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);
    }

    @Test(expected = AgentErr.NotAvailableException.class)
    public void should_raise_exception_agent_busy() throws InterruptedException {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-004";

        // when: create node and send command to agent
        zkClient.createEphemeral(ZKHelper.buildPath(zoneName, agentName), null);
        Thread.sleep(1000);

        CmdInfo cmd = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);

        // then: send command to agent again should raise AgentErr.BusyException.class
        cmdService.send(cmd);
    }

    @Test
    public void should_write_cmd_log() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-005";

        CmdInfo baseInfo = new CmdInfo(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        Cmd created = cmdService.create(baseInfo);

        byte[] mockData = "test".getBytes();
        String originalFilename = created.getId() + ".out.zip";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", originalFilename, "application/zip", mockData);

        // when:
        cmdService.saveLog(created.getId(), mockMultipartFile);

        // then:
        Assert.assertTrue(Files.exists(Paths.get(AppConfig.CMD_LOG_DIR.toString(), originalFilename)));
    }

    @Test
    public void should_create_session_and_send_cmd_with_session() throws Throwable {
        // given:
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-006";
        zkClient.createEphemeral(ZKHelper.buildPath(zoneName, agentName), null);
        Thread.sleep(1000);

        // when: send cmd for create agent session
        Cmd cmd = cmdService.send(new CmdInfo(zoneName, agentName, CmdType.CREATE_SESSION, null));
        Assert.assertNotNull(cmd.getSessionId());

        // then: check agent is locked by session
        Agent target = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, target.getStatus());
        Assert.assertNotNull(target.getSessionId());
        Assert.assertEquals(cmd.getSessionId(), target.getSessionId());

        // when: send cmd to create agent session again should fail since agent not available
        try {
            cmdService.send(new CmdInfo(zoneName, agentName, CmdType.CREATE_SESSION, null));
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }

        // when: send cmd with session id
        CmdInfo cmdWithSession = new CmdInfo(zoneName, null, CmdType.RUN_SHELL, "echo hello");
        cmdWithSession.setSessionId(target.getSessionId());
        cmd = cmdService.send(cmdWithSession);

        // then: check target is found correctly by session id
        Agent sessionAgent = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(target, sessionAgent);
        Assert.assertEquals(target.getSessionId(), sessionAgent.getSessionId());
        Assert.assertEquals(AgentStatus.BUSY, sessionAgent.getStatus());
        Assert.assertNotNull(sessionAgent.getSessionDate());

        // then: mock cmd been executed
        CmdStatusItem statusItem = new CmdStatusItem(cmd.getId(), CmdStatus.LOGGED, new CmdResult(), true, true);
        cmdService.updateStatus(statusItem, false);

        // when: delete session
        CmdInfo cmdToDelSession = new CmdInfo(zoneName, null, CmdType.DELETE_SESSION, null);
        cmdToDelSession.setSessionId(cmd.getSessionId());
        cmd = cmdService.send(cmdToDelSession);

        Agent sessionShouldReleased = agentService.find(cmd.getAgentPath());
        Assert.assertNull(sessionShouldReleased.getSessionId());
        Assert.assertEquals(AgentStatus.IDLE, sessionShouldReleased.getStatus());
    }
}
