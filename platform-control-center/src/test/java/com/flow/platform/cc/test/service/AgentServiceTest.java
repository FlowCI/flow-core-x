package com.flow.platform.cc.test.service;

import com.flow.platform.cc.cloud.MosInstanceManager;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.AgentServiceImpl;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class AgentServiceTest extends TestBase {

    private final static String MOCK_PROVIDER_NAME = "mock-cloud-provider";

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private SpringContextUtil springContextUtil;

    @Test
    public void should_agent_initialized() throws InterruptedException, KeeperException {
        // given:
        String zoneName = "ut-test-zone-1";
        zoneService.createZone(new Zone(zoneName, MOCK_PROVIDER_NAME));
        Assert.assertEquals(0, agentService.onlineList(zoneName).size());

        String agentName = "test-agent-001";
        ZkPathBuilder builder = zkHelper.buildZkPath(zoneName, agentName);

        // when: simulate to create agent
        ZkNodeHelper.createEphemeralNode(zkClient, builder.path(), "");

        // then:
        Thread.sleep(2000);
        Assert.assertEquals(1, agentService.onlineList(zoneName).size());
        Assert.assertTrue(agentService.onlineList(zoneName).contains(new Agent(zoneName, agentName)));
    }

    @Test
    public void should_batch_report_agent() throws Throwable {
        // given: define zones
        String zone_1 = "zone-1";
        zoneService.createZone(new Zone(zone_1, MOCK_PROVIDER_NAME));
        String zone_2 = "zone-2";
        zoneService.createZone(new Zone(zone_2, MOCK_PROVIDER_NAME));

        // when: agents online to zone-1
        AgentPath agent11 = new AgentPath(zone_1, "agent-1");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent11), "");

        AgentPath agent12 = new AgentPath(zone_1, "agent-2");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent12), "");
        Thread.sleep(1000); // waiting for watcher call

        // then: should has two online agent in zone-1
        Assert.assertEquals(2, agentService.onlineList(zone_1).size());

        // when: agent online to zone-2
        AgentPath agent2 = new AgentPath(zone_2, "agent-1");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent2), "");
        Thread.sleep(1000); // waiting for watcher call

        // then: should has one online agent in zone-2
        Assert.assertEquals(1, agentService.onlineList(zone_2).size());

        // then: still has two online agent in zone-1
        Assert.assertEquals(2, agentService.onlineList(zone_1).size());

        // when: there is one agent offline in zone-1
        ZkNodeHelper.deleteNode(zkClient, zkHelper.getZkPath(agent12));
        Thread.sleep(1000); // waiting for watcher call

        // then: should left one agent in zone-1
        Assert.assertEquals(1, agentService.onlineList(zone_1).size());
        Agent agent11Loaded = (Agent) agentService.onlineList(zone_1).toArray()[0];
        Assert.assertEquals(agent11, agent11Loaded.getPath());
    }

    @Test
    public void should_report_agent_status() throws InterruptedException {
        // given: init zk agent
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-for-status";
        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
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
        String zoneName = zkHelper.getZones().get(0).getName();
        String agentName = "test-agent-for-status-exception";

        AgentPath pathObj = new AgentPath(zoneName, agentName);
        agentService.reportStatus(pathObj, Agent.Status.BUSY);
    }

    @Ignore
    @Test
    public void should_start_instance_when_pool_size_less_than_min() throws Throwable {
        // given:
        final String zoneName = "test-mos-mac";
        final int minPoolSize = 1;

        Zone zone = new Zone(zoneName, "mos");
        MosInstanceManager instanceManager = (MosInstanceManager) springContextUtil.getBean("mosInstanceManager");

        // when: check and start instance to make sure min idle pool size
        AgentServiceImpl agentService = (AgentServiceImpl) this.agentService;
        agentService.keepIdleAgentMinSize(zone, instanceManager, minPoolSize);

        // then: wait for 30 seconds and check running instance
        Thread.sleep(1000 * 30);
        Assert.assertEquals(1, instanceManager.runningInstance().size());
    }

    @Test
    public void should_clean_up_agent_when_pool_size_over_max() throws Throwable {
        // given: mock to start num of agent over pool size
        final int maxPoolSize = 2;
        final String mockAgentNamePattern = "mock-agent-%d";
        final String zoneName = "test-mos-mac";

        Zone zone = new Zone(zoneName, "mos");
        MosInstanceManager instanceManager = (MosInstanceManager) springContextUtil.getBean("mosInstanceManager");

        for (int i = 0; i < maxPoolSize + 1; i++) {
            String path = zkHelper.buildZkPath(zone.getName(), String.format(mockAgentNamePattern, i)).path();
            ZkNodeHelper.createEphemeralNode(zkClient, path, "");
            Thread.sleep(1000); // wait for agent start and make them in time sequence
        }

        // when: shutdown instance which over the max agent pool size
        AgentServiceImpl agentService = (AgentServiceImpl) this.agentService;
        agentService.keepIdleAgentMaxSize(zone, instanceManager, maxPoolSize);

        // then: check shutdown cmd should be sent
        ZkPathBuilder mockAgent0Path = zkHelper.buildZkPath(zoneName, String.format(mockAgentNamePattern, 0));
        byte[] shutdownCmdRaw = ZkNodeHelper.getNodeData(zkClient, mockAgent0Path.path(), null);
        Cmd shutdownCmd = Cmd.parse(shutdownCmdRaw, Cmd.class);
        Assert.assertEquals(Cmd.Type.SHUTDOWN, shutdownCmd.getType());
        Assert.assertEquals(Agent.Status.OFFLINE, agentService.find(shutdownCmd.getAgentPath()).getStatus());

        ZkPathBuilder mockAgent1Path = zkHelper.buildZkPath(zoneName, String.format(mockAgentNamePattern, 1));
        Assert.assertEquals(0, ZkNodeHelper.getNodeData(zkClient, mockAgent1Path.path(), null).length);

        ZkPathBuilder mockAgent2Path = zkHelper.buildZkPath(zoneName, String.format(mockAgentNamePattern, 2));
        Assert.assertEquals(0, ZkNodeHelper.getNodeData(zkClient, mockAgent2Path.path(), null).length);
    }

    @After
    public void after() {
        MosInstanceManager instanceManager = (MosInstanceManager) springContextUtil.getBean("mosInstanceManager");
        instanceManager.cleanAll();
    }
}
