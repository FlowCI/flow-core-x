package com.flow.platform.cc.test;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.ZkService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZkServiceTest extends TestBase {

    @Autowired
    private ZkService zkService;

    @Test
    public void should_zk_service_initialized() {
        String[] zones = zkZone.split(";");
        for (String zoneName : zones) {
            String zonePath = ZkPathBuilder.create("flow-agents").append(zoneName).path();
            Assert.assertTrue(ZkNodeHelper.exist(zkClient, zonePath) != null);
        }
    }

    @Test
    public void should_agent_initialized() throws InterruptedException, KeeperException {
        // given:
        String zoneName = zkZone.split(";")[0];
        String agentName = "test-agent-001";
        Assert.assertEquals(0, zkService.onlineAgent(zoneName).size());

        ZkPathBuilder builder = ZkPathBuilder.create("flow-agents").append(zoneName).append(agentName);

        // when: simulate to create agent
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        ZkNodeHelper.createEphemeralNode(zkClient, builder.busy(), "");

        // then:
        Thread.sleep(2000);
        Assert.assertEquals(2, zkService.onlineAgent(zoneName).size());
        Assert.assertTrue(zkService.onlineAgent(zoneName).contains(agentName));
        Assert.assertTrue(zkService.onlineAgent(zoneName).contains(agentName + "-busy"));
    }

    @Test
    public void should_send_cmd_to_agent() throws InterruptedException {
        // given:
        String zoneName = zkZone.split(";")[0];
        String agentName = "test-agent-002";

        String agentPath = ZkPathBuilder.create("flow-agents").append(zoneName).append(agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // when: send command
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        Cmd cmdInfo = zkService.sendCommand(cmd);

        Assert.assertNotNull(cmdInfo.getId());
        Assert.assertEquals(Cmd.Status.PENDING, cmdInfo.getStatus());
        Assert.assertEquals(zoneName, cmdInfo.getZone());
        Assert.assertEquals(agentName, cmdInfo.getAgent());

        // then:
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, agentPath, null);
        Cmd loaded = Cmd.parse(raw);
        Assert.assertEquals(cmdInfo, loaded);
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_exception_agent_not_exit() {
        // given:
        String zoneName = zkZone.split(";")[0];
        String agentName = "test-agent-003";

        // then: send command immediately should raise AgentErr.NotFoundException
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        zkService.sendCommand(cmd);
    }

    @Test(expected = AgentErr.BusyException.class)
    public void should_raise_exception_agent_busy() throws InterruptedException {
        // given:
        String zoneName = zkZone.split(";")[0];
        String agentName = "test-agent-004";

        ZkPathBuilder builder = ZkPathBuilder.create("flow-agents").append(zoneName).append(agentName);

        // when: create node and set busy state
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");
        ZkNodeHelper.createEphemeralNode(zkClient, builder.busy(), "");
        Thread.sleep(1000);

        // then: send command to agent should raise AgentErr.BusyException.class
        CmdBase cmd = new CmdBase(zoneName, agentName, Cmd.Type.RUN_SHELL, "/test.sh");
        zkService.sendCommand(cmd);
    }
}