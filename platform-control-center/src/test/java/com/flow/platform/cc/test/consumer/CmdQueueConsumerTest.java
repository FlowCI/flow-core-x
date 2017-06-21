package com.flow.platform.cc.test.consumer;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.rabbitmq.client.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
public class CmdQueueConsumerTest extends TestBase {

    @Autowired
    private Channel cmdSendChannel;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Value("${mq.exchange.name}")
    private String cmdExchangeName;

    @Test
    public void should_receive_cmd_via_queue() throws Throwable {
        // given:
        String zoneName = "ut-test-zone-for-queue";
        zoneService.createZone(new Zone(zoneName, "mock-cloud-provider"));

        String agentName = "mock-agent-1";
        AgentPath agentPath = new AgentPath(zoneName, agentName);
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agentPath), "");
        Thread.sleep(1000);

        // when: send cmd by rabbit mq with cmd exchange name
        CmdBase mockCmd = new CmdBase(zoneName, agentName, CmdType.RUN_SHELL, "echo hello");
        cmdSendChannel.basicPublish(cmdExchangeName, "", null, mockCmd.toBytes());
        Thread.sleep(1000);

        // then: cmd should received in zookeeper agent node
        byte[] raw = zkClient.getData(zkHelper.getZkPath(agentPath), false, null);
        CmdBase received = CmdBase.parse(raw, CmdBase.class);
        Assert.assertEquals(mockCmd, received);
    }
}
