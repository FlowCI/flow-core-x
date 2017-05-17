package com.flow.platform.cc.test;

import com.flow.platform.cc.service.ZkService;
import com.flow.platform.util.zk.ZkCmd;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.gson.Gson;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
public class ZkServiceTest extends TestBase {

    @Autowired
    private ZkService zkService;

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    @Value("${zk.node.zone}")
    private String zkZone;

    private ZooKeeper zkClient;

    @Before
    public void before() throws IOException {
        zkClient = new ZooKeeper(zkHost, zkTimeout, null);
    }

    @Test
    public void should_zk_service_initialized() {
        String[] zones = zkZone.split(";");
        for (String zone : zones) {
            String zonePath = "/flow-agents/" + zone;
            Assert.assertTrue(ZkNodeHelper.exist(zkClient, zonePath) != null);
        }
    }

    @Test
    public void should_agent_initialized() throws InterruptedException {
        // given:
        String zoneName = zkZone.split(";")[0];
        Assert.assertEquals(0, zkService.onlineAgent(zoneName).size());

        String agentPath = String.format("/flow-agents/%s/%s", zoneName, "test-agent-001");
        String agentPathBusy = String.format("/flow-agents/%s/%s", zoneName, "test-agent-001-busy");

        // when: simulate to create agent
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        ZkNodeHelper.createEphemeralNode(zkClient, agentPathBusy, "");

        // then:
        Thread.sleep(2000);
        Assert.assertEquals(2, zkService.onlineAgent(zoneName).size());
        Assert.assertTrue(zkService.onlineAgent(zoneName).contains("test-agent-001"));
        Assert.assertTrue(zkService.onlineAgent(zoneName).contains("test-agent-001-busy"));
    }

    @Test
    public void should_send_cmd_to_agent() throws InterruptedException {
        // given:
        String zoneName = zkZone.split(";")[0];
        String agentName = "test-agent-001";
        Assert.assertEquals(0, zkService.onlineAgent(zoneName).size());

        String agentPath = String.format("/flow-agents/%s/%s", zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(1000);

        // when: send command
        ZkCmd cmd = new ZkCmd(ZkCmd.Type.RUN_SHELL, "/test.sh");
        zkService.sendCommand(zoneName, agentName, cmd);

        // then:
        byte[] raw = ZkNodeHelper.getNodeData(zkClient, agentPath, null);
        Gson gson = new Gson();
        ZkCmd loaded = gson.fromJson(new String(raw), ZkCmd.class);

        Assert.assertEquals(cmd, loaded);
    }

    @Test
    public void should_raise_exception_agent_not_exit() {

    }

    @Test
    public void should_raise_exception_agent_busy() {

    }
}
