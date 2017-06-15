package com.flow.platform.cc.test.service;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.DateUtil;
import com.flow.platform.domain.*;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import static junit.framework.TestCase.fail;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CmdServiceTest extends TestBase {

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private Queue<Path> cmdLoggingQueue;

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

    @Before
    public void before() {
        cmdLoggingQueue.clear();
    }

    @Test
    public void should_create_cmd() {
        // given:
        CmdBase base = new CmdBase("test-zone", "test-agent", CmdType.KILL, null);

        // when:
        Cmd cmd = cmdService.create(base);
        Assert.assertNotNull(cmd);
        Assert.assertNotNull(cmd.getId());
        Assert.assertNotNull(cmd.getCreatedDate());
        Assert.assertNotNull(cmd.getUpdatedDate());

        // then:
        Cmd loaded = cmdService.find(cmd.getId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(cmd, loaded);
    }

    @Test
    public void should_report_cmd_status() {
        // given:
        AgentPath agentPath = new AgentPath("test-zone", "test-agent");
        agentService.reportOnline("test-zone", Lists.newArrayList(agentPath));

        CmdBase base = new CmdBase(agentPath, CmdType.RUN_SHELL, null);
        Cmd cmd = cmdService.create(base);
        Assert.assertNotNull(cmd);
        Assert.assertNotNull(cmd.getId());

        // when:
        CmdResult result = new CmdResult();
        result.setStartTime(new Date());
        result.setProcess(mockProcess);
        cmdService.report(cmd.getId(), CmdStatus.RUNNING, result);

        // then: check cmd status should be running and agent status should be busy
        Cmd loaded = cmdService.find(cmd.getId());

        Assert.assertTrue(loaded.getStatus().equals(CmdStatus.RUNNING));
        Assert.assertNotNull(loaded.getResult());
        Assert.assertEquals(mockProcess, loaded.getResult().getProcess());

        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentPath).getStatus());
    }

    @Test
    public void should_cmd_timeout() throws Throwable {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-for-timeout";

        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(500);

        // when: send cmd
        Cmd cmd = cmdService.send(new CmdBase(zoneName, null, CmdType.RUN_SHELL, "test"));
        Assert.assertTrue(cmd.isCurrent());

        // then: check should not timeout
        Assert.assertEquals(false, cmdService.isTimeout(cmd));

        // when: mock cmd timeout
        Date timeoutDate = DateUtil.toDate(ZonedDateTime.now().minusSeconds(CmdService.CMD_TIMEOUT_SECONDS + 10));
        cmd.setCreatedDate(timeoutDate);
        cmd.setStatus(CmdStatus.RUNNING);

        // then: should timeout and status should be TIMEOUT_KILL
        Assert.assertEquals(true, cmdService.isTimeout(cmd));
        cmdService.checkTimeoutTask();
        Assert.assertEquals(CmdStatus.TIMEOUT_KILL, cmdService.find(cmd.getId()).getStatus());
    }

    @Test
    public void should_update_agent_status_by_cmd_status() throws Throwable {
        // given
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-001";

        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // should Agent.status.IDLE from cmd finish status
        for (CmdStatus reportStatus : Cmd.FINISH_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdBase base = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            cmdService.report(current.getId(), reportStatus, new CmdResult());

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current, loaded);

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.IDLE, relatedAgent.getStatus());
        }

        // Agent.status.BUSY from cmd working status
        for (CmdStatus status : Cmd.WORKING_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdBase base = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            cmdService.report(current.getId(), status, new CmdResult());

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current, loaded);

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(AgentStatus.BUSY, relatedAgent.getStatus());

            // reset agent status
            relatedAgent.setStatus(AgentStatus.IDLE);
        }
    }

    @Test
    public void should_send_cmd_to_agent() throws InterruptedException {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-002";

        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // when: send command
        CmdBase cmd = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        Cmd cmdInfo = cmdService.send(cmd);

        // then:

        // check cmd status
        Assert.assertNotNull(cmdInfo.getId());
        Assert.assertTrue(cmdInfo.getStatus().equals(CmdStatus.PENDING));
        Assert.assertEquals(zoneName, cmdInfo.getZone());
        Assert.assertEquals(agentName, cmdInfo.getAgent());

        // check cmd been recorded
        Assert.assertTrue(cmdService.listByAgentPath(cmd.getAgentPath()).contains(cmdInfo));

        // check agent status
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(cmd.getAgentPath()).getStatus());

        // check zk node received the same cmd
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, agentPath, null);
        Cmd loaded = Jsonable.parse(raw, Cmd.class);
        Assert.assertEquals(cmdInfo, loaded);

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

    @Test
    public void should_auto_select_idle_agent_when_cmd_send() throws Throwable {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        AgentPath agentIdle1 = new AgentPath(zoneName, "idle-agent-01");
        AgentPath agentIdle2 = new AgentPath(zoneName, "idle-agent-02");
        AgentPath agentBusy1 = new AgentPath(zoneName, "busy-agent-01");

        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agentIdle1), "");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agentIdle2), "");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agentBusy1), "");
        Thread.sleep(1000);

        // report busy status
        cmdService.send(new CmdBase(agentBusy1, CmdType.RUN_SHELL, "echo \"hello\""));
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentBusy1).getStatus());

        // set idle agent 1 date, before idle agent 2
        Instant date = LocalDate.of(2017, 5, 10).atStartOfDay(ZoneId.systemDefault()).toInstant();
        agentService.find(agentIdle1).setUpdatedDate(Date.from(date));

        // when: send cmd to zone
        Cmd cmdForIdle1 = cmdService.send(new CmdBase(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));

        // then: should select agent idle 1 as target
        Assert.assertEquals(agentIdle1, cmdForIdle1.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentIdle1).getStatus());

        // when: send cmd to make all agent to busy
        Cmd cmdForIdle2 = cmdService.send(new CmdBase(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));
        Assert.assertEquals(agentIdle2, cmdForIdle2.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, agentService.find(agentIdle2).getStatus());

        // then: should raise NotAvailableException
        try {
            cmdService.send(new CmdBase(zoneName, null, CmdType.RUN_SHELL, "echo \"hello\""));
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_exception_agent_not_exit() {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-003";

        // then: send command immediately should raise AgentErr.NotFoundException
        CmdBase cmd = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);
    }

    @Test(expected = AgentErr.NotAvailableException.class)
    public void should_raise_exception_agent_busy() throws InterruptedException {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-004";

        // when: create node and send command to agent
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        Thread.sleep(1000);

        CmdBase cmd = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);

        // then: send command to agent again should raise AgentErr.BusyException.class
        cmdService.send(cmd);
    }

    @Test
    public void should_write_cmd_log() throws Throwable {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-005";

        CmdBase baseInfo = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, "/test.sh");
        Cmd created = cmdService.create(baseInfo);

        byte[] mockData = "test".getBytes();
        String originalFilename = created.getId() + ".out.zip";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", originalFilename, "application/zip", mockData);

        // when:
        cmdService.saveFullLog(created.getId(), mockMultipartFile);

        // then:
        Assert.assertTrue(Files.exists(Paths.get(AppConfig.CMD_LOG_DIR.toString(), originalFilename)));
        Assert.assertEquals(1, cmdLoggingQueue.size());
    }

    @Test
    public void should_create_session_and_send_cmd_with_session() throws Throwable {
        // given:
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-006";
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.buildZkPath(zoneName, agentName).path(), "");
        Thread.sleep(1000);

        // when: send cmd for create agent session
        Cmd cmd = cmdService.send(new CmdBase(zoneName, agentName, CmdType.CREATE_SESSION, null));
        Assert.assertNotNull(cmd.getSessionId());

        // then: check agent is locked by session
        Agent target = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(AgentStatus.BUSY, target.getStatus());
        Assert.assertNotNull(target.getSessionId());
        Assert.assertEquals(cmd.getSessionId(), target.getSessionId());

        // when: send cmd to create agent session again should fail since agent not available
        try {
            cmdService.send(new CmdBase(zoneName, agentName, CmdType.CREATE_SESSION, null));
            fail();
        } catch (Throwable e) {
            Assert.assertEquals(AgentErr.NotAvailableException.class, e.getClass());
        }

        // when: send cmd with session id
        CmdBase cmdWithSession = new CmdBase(zoneName, null, CmdType.RUN_SHELL, "echo hello");
        cmdWithSession.setSessionId(target.getSessionId());
        cmd = cmdService.send(cmdWithSession);

        // then: check target is found correctly by session id
        Agent sessionAgent = agentService.find(cmd.getAgentPath());
        Assert.assertEquals(target, sessionAgent);
        Assert.assertEquals(target.getSessionId(), sessionAgent.getSessionId());
        Assert.assertEquals(AgentStatus.BUSY, sessionAgent.getStatus());
        Assert.assertNotNull(sessionAgent.getSessionDate());

        // then: mock cmd been executed
        cmd.addStatus(CmdStatus.LOGGED);

        // when: delete session
        CmdBase cmdToDelSession = new CmdBase(zoneName, null, CmdType.DELETE_SESSION, null);
        cmdToDelSession.setSessionId(cmd.getSessionId());
        cmd = cmdService.send(cmdToDelSession);

        Agent sessionShouldReleased = agentService.find(cmd.getAgentPath());
        Assert.assertNull(sessionShouldReleased.getSessionId());
        Assert.assertEquals(AgentStatus.IDLE, sessionShouldReleased.getStatus());
    }
}
