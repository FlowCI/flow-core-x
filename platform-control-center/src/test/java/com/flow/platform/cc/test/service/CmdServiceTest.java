package com.flow.platform.cc.test.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZkHelper;
import com.flow.platform.domain.*;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

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
    private ZoneService zoneService;

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
        CmdBase base = new CmdBase("test-zone", "test-agent", Cmd.Type.KILL, null);

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

        CmdBase base = new CmdBase(agentPath, Cmd.Type.RUN_SHELL, null);
        Cmd cmd = cmdService.create(base);
        Assert.assertNotNull(cmd);
        Assert.assertNotNull(cmd.getId());

        // when:
        CmdResult result = new CmdResult();
        result.setStartTime(new Date());
        result.setProcess(mockProcess);
        cmdService.report(cmd.getId(), Cmd.Status.RUNNING, result);

        // then: check cmd status should be running and agent status should be busy
        Cmd loaded = cmdService.find(cmd.getId());

        Assert.assertEquals(Cmd.Status.RUNNING, loaded.getStatus());
        Assert.assertNotNull(loaded.getResult());
        Assert.assertEquals(mockProcess, loaded.getResult().getProcess());

        Assert.assertEquals(Agent.Status.BUSY, agentService.find(agentPath).getStatus());
    }

    @Test
    public void should_update_agent_status_by_cmd_status() throws Throwable {
        // given
        String zoneName = zkHelper.getZones()[0];
        String agentName = "test-agent-001";

        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // should Agent.status.IDLE from cmd finish status
        for (Cmd.Status reportStatus : Cmd.FINISH_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdBase base = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(Agent.Status.BUSY, relatedAgent.getStatus());

            cmdService.report(current.getId(), reportStatus, new CmdResult());

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current, loaded);

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(Agent.Status.IDLE, relatedAgent.getStatus());
        }

        // Agent.status.BUSY from cmd working status
        for (Cmd.Status status : Cmd.WORKING_STATUS) {

            // when: send cmd status and mock to report cmd status
            CmdBase base = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, null);
            Cmd current = cmdService.send(base);

            Agent relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(Agent.Status.BUSY, relatedAgent.getStatus());

            cmdService.report(current.getId(), status, new CmdResult());

            // then:
            Cmd loaded = cmdService.find(current.getId());
            Assert.assertEquals(current, loaded);

            relatedAgent = agentService.find(base.getAgentPath());
            Assert.assertEquals(Agent.Status.BUSY, relatedAgent.getStatus());

            // reset agent status
            relatedAgent.setStatus(Agent.Status.IDLE);
        }
    }

    @Test
    public void should_send_cmd_to_agent() throws InterruptedException {
        // given:
        String zoneName = zkHelper.getZones()[0];
        String agentName = "test-agent-002";

        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // when: send command
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        Cmd cmdInfo = cmdService.send(cmd);

        Assert.assertNotNull(cmdInfo.getId());
        Assert.assertEquals(Cmd.Status.PENDING, cmdInfo.getStatus());
        Assert.assertEquals(zoneName, cmdInfo.getZone());
        Assert.assertEquals(agentName, cmdInfo.getAgent());

        // then:
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, agentPath, null);
        Cmd loaded = Jsonable.parse(raw, Cmd.class);
        Assert.assertEquals(cmdInfo, loaded);
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_exception_agent_not_exit() {
        // given:
        String zoneName = zkHelper.getZones()[0];
        String agentName = "test-agent-003";

        // then: send command immediately should raise AgentErr.NotFoundException
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);
    }

    @Test(expected = AgentErr.NotAvailableException.class)
    public void should_raise_exception_agent_busy() throws InterruptedException {
        // given:
        String zoneName = zkHelper.getZones()[0];
        String agentName = "test-agent-004";

        // when: create node and send command to agent
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        Thread.sleep(1000);

        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        cmdService.send(cmd);

        // then: send command to agent again should raise AgentErr.BusyException.class
        cmdService.send(cmd);
    }
}
