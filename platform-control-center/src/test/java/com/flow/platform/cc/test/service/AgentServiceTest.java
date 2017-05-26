package com.flow.platform.cc.test.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Lists;
import org.apache.zookeeper.KeeperException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
public class AgentServiceTest extends TestBase {

    @Autowired
    private AgentService agentService;

    @Test
    public void should_agent_initialized() throws InterruptedException, KeeperException {
        // given:
        String zoneName = "ut-test-zone-1";
        zkService.createZone(zoneName);
        Assert.assertEquals(0, agentService.onlineList(zoneName).size());

        String agentName = "test-agent-001";
        ZkPathBuilder builder = zkService.buildZkPath(zoneName, agentName);

        // when: simulate to create agent
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

        // then:
        Thread.sleep(2000);
        Assert.assertEquals(1, agentService.onlineList(zoneName).size());
        Assert.assertTrue(agentService.onlineList(zoneName).contains(new Agent(zoneName, agentName)));
    }

    @Test
    public void should_batch_report_agent() {
        // when: report agent online to zone-1
        AgentPath agent11 = new AgentPath("zone-1", "agent-1");
        agentService.reportOnline("zone-1", Lists.newArrayList(agent11));

        AgentPath agent12 = new AgentPath("zone-1", "agent-2");
        agentService.reportOnline("zone-1", Lists.newArrayList(agent11, agent12));

        // then: should has two online agent in zone-1
        Assert.assertEquals(2, agentService.onlineList("zone-1").size());

        // when: report agent online to zone-2
        AgentPath agent2 = new AgentPath("zone-2", "agent-1");
        agentService.reportOnline("zone-2", Lists.newArrayList(agent2));

        // then: should has one online agent in zone-2
        Assert.assertEquals(1, agentService.onlineList("zone-2").size());

        // then: still has two online agent in zone-1
        Assert.assertEquals(2, agentService.onlineList("zone-1").size());

        // when: there is one agent offline in zone-1
        agentService.reportOnline("zone-1", Lists.newArrayList(agent11));

        // then: should left one agent in zone-1
        Assert.assertEquals(1, agentService.onlineList("zone-1").size());
        Agent agent11Loaded = (Agent) agentService.onlineList("zone-1").toArray()[0];
        Assert.assertEquals(agent11, agent11Loaded.getPath());
    }

    @Test
    public void should_report_agent_status() throws InterruptedException {
        // given: init zk agent
        String zoneName = zkService.definedZones()[0];
        String agentName = "test-agent-for-status";
        String agentPath = zkService.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(500);

        // when: report status
        AgentPath pathObj = new AgentPath(zoneName, agentName);
        agentService.reportStatus(pathObj, Agent.Status.BUSY);

        // then:
        Agent exit = agentService.find(pathObj);
        Assert.assertEquals(Agent.Status.BUSY, exit.getStatus());
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_not_found_exception_when_report_status() {
        String zoneName = zkService.definedZones()[0];
        String agentName = "test-agent-for-status-exception";

        AgentPath pathObj = new AgentPath(zoneName, agentName);
        agentService.reportStatus(pathObj, Agent.Status.BUSY);
    }
}
