package com.flow.platform.cc.test.service;

import com.flow.platform.cc.cloud.MosInstanceManager;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.service.ZoneServiceImpl;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by gy@fir.im on 06/06/2017.
 * Copyright fir.im
 */
public class ZoneServiceTest extends TestBase {

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpringContextUtil springContextUtil;

    @Test
    public void should_create_and_get_zones() {
        // when: create zone;
        String path1 = zoneService.createZone(new Zone("my-test-zone-1", "mock-provider-name"));
        Assert.assertNotNull(path1);
        Assert.assertNotNull("/flow-agents/my-test-zone-1", path1);

        String path2 = zoneService.createZone(new Zone("my-test-zone-2", "mock-provider-name"));
        Assert.assertNotNull(path2);
        Assert.assertNotNull("/flow-agents/my-test-zone-2", path2);

        // then:
        List<Zone> zones = zoneService.getZones();
        Assert.assertNotNull(zones);
        Assert.assertTrue(zones.size() >= 4); // 2 for default, 2 for created
    }

    @Test
    public void should_find_instance_manager() {
        // when:
        Zone mosZone = new Zone("mos-ut", "mos");
        mosZone.setImageName("test-image-name");
        zoneService.createZone(mosZone);

        // then:
        Zone found = zoneService.getZone("mos-ut");
        Assert.assertNotNull(found);
        Assert.assertTrue(zoneService.findInstanceManager(found) instanceof MosInstanceManager);
    }

    @Test
    public void should_start_instance_when_pool_size_less_than_min() throws Throwable {
        // given:
        final String zoneName = "test-mos-mac";

        Zone zone = new Zone(zoneName, "mos");
        zone.setMinPoolSize(1);
        zone.setImageName("flow-osx-83-109-bj4-zk-agent");
        MosInstanceManager instanceManager = (MosInstanceManager) springContextUtil
            .getBean("mosInstanceManager");

        // when: check and start instance to make sure min idle pool size
        zoneService.keepIdleAgentMinSize(zone, instanceManager);

        // then: wait for 30 seconds and check running instance
        Thread.sleep(1000 * 30);
        Assert.assertEquals(1, instanceManager.instances().size());
    }

    @Test
    public void should_clean_up_agent_when_pool_size_over_max() throws Throwable {
        // given: mock to start num of agent over pool size
        final String mockAgentNamePattern = "mock-agent-%d";
        final String zoneName = "test-mos-mac";

        Zone zone = new Zone(zoneName, "mos");
        zone.setMinPoolSize(1);
        zone.setMaxPoolSize(2);
        MosInstanceManager instanceManager =
            (MosInstanceManager) springContextUtil.getBean("mosInstanceManager");

        for (int i = 0; i < zone.getMaxPoolSize() + 1; i++) {
            String path = zkHelper
                .buildZkPath(zone.getName(), String.format(mockAgentNamePattern, i)).path();
            ZkNodeHelper.createEphemeralNode(zkClient, path, "");
            Thread.sleep(1000); // wait for agent start and make them in time sequence
        }

        // when: shutdown instance which over the max agent pool size
        zoneService.keepIdleAgentMaxSize(zone, instanceManager);

        // then: check shutdown cmd should be sent
        ZkPathBuilder mockAgent0Path = zkHelper
            .buildZkPath(zoneName, String.format(mockAgentNamePattern, 0));
        byte[] shutdownCmdRaw = ZkNodeHelper.getNodeData(zkClient, mockAgent0Path.path(), null);
        Cmd shutdownCmd = Cmd.parse(shutdownCmdRaw, Cmd.class);
        Assert.assertNotNull(shutdownCmd);
        Assert.assertEquals(CmdType.SHUTDOWN, shutdownCmd.getType());
        Assert.assertEquals(AgentStatus.OFFLINE,
            agentService.find(shutdownCmd.getAgentPath()).getStatus());

        ZkPathBuilder mockAgent1Path = zkHelper
            .buildZkPath(zoneName, String.format(mockAgentNamePattern, 1));
        Assert.assertEquals(0,
            ZkNodeHelper.getNodeData(zkClient, mockAgent1Path.path(), null).length);

        ZkPathBuilder mockAgent2Path = zkHelper
            .buildZkPath(zoneName, String.format(mockAgentNamePattern, 2));
        Assert.assertEquals(0,
            ZkNodeHelper.getNodeData(zkClient, mockAgent2Path.path(), null).length);
    }

    @After
    public void after() {
        MosInstanceManager instanceManager =
            (MosInstanceManager) springContextUtil.getBean("mosInstanceManager");
        instanceManager.cleanAll();
    }
}
