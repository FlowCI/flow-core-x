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

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import java.time.ZonedDateTime;
import org.apache.zookeeper.KeeperException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class AgentServiceTest extends TestBase {

    private final static String MOCK_PROVIDER_NAME = "mock-cloud-provider";

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

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
        Thread.sleep(1000);
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

        Thread.sleep(1000); // mock network delay

        AgentPath agent12 = new AgentPath(zone_1, "agent-2");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent12), "");

        Thread.sleep(1000); // mock network delay

        /* mock agent13 with exit offline status */
        AgentPath agent13 = new AgentPath(zone_1, "agent-3");
        Agent agentWithOffline = new Agent(agent13);
        agentWithOffline.setCreatedDate(DateUtil.now());
        agentWithOffline.setUpdatedDate(DateUtil.now());
        agentWithOffline.setStatus(AgentStatus.OFFLINE);
        agentDao.save(agentWithOffline);
        Assert.assertNotNull(agentService.find(agent13));

        /* make agent13 online again */
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent13), "");

        Thread.sleep(2000); // waiting for watcher call

        // then: should has three online agent in zone-1
        Assert.assertEquals(3, agentService.onlineList(zone_1).size());

        // when: agent online to zone-2
        AgentPath agent2 = new AgentPath(zone_2, "agent-1");
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agent2), "");
        Thread.sleep(1000); // waiting for watcher call

        // then: should has one online agent in zone-2
        Assert.assertEquals(1, agentService.onlineList(zone_2).size());

        // then: still has three online agent in zone-1
        Assert.assertEquals(3, agentService.onlineList(zone_1).size());

        // when: there is one agent offline in zone-1
        ZkNodeHelper.deleteNode(zkClient, zkHelper.getZkPath(agent12));
        Thread.sleep(1000); // waiting for watcher call

        // then: should left two agent in zone-1
        Assert.assertEquals(2, agentService.onlineList(zone_1).size());
        Agent agent11Loaded = (Agent) agentService.onlineList(zone_1).toArray()[0];
        Assert.assertEquals(agent11, agent11Loaded.getPath());
    }

    @Test
    public void should_report_agent_status() throws InterruptedException {
        // given: init zk agent
        String zoneName = zkHelper.getDefaultZones().get(0).getName();
        String agentName = "test-agent-for-status";
        String agentPath = zkHelper.buildZkPath(zoneName, agentName).path();
        ZkNodeHelper.createEphemeralNode(zkClient, agentPath, "");
        Thread.sleep(500);

        // when: report status
        AgentPath pathObj = new AgentPath(zoneName, agentName);
        agentService.updateStatus(pathObj, AgentStatus.BUSY);

        // then:
        Agent exit = agentService.find(pathObj);
        Assert.assertEquals(AgentStatus.BUSY, exit.getStatus());
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_not_found_exception_when_report_status() {
        String zoneName = zkHelper.getDefaultZones().get(0).getName();
        String agentName = "test-agent-for-status-exception";

        AgentPath pathObj = new AgentPath(zoneName, agentName);
        agentService.updateStatus(pathObj, AgentStatus.BUSY);
    }

    @Test
    public void should_agent_session_timeout() throws Throwable {
        // when:
        Agent mockAgent = new Agent("test-zone", "session-timeout-agent");
        mockAgent.setSessionId("mock-session-id");
        mockAgent.setSessionDate(ZonedDateTime.now());

        // then:
        Thread.sleep(1500); // wait for 2 seconds
        Assert.assertTrue(agentService.isSessionTimeout(mockAgent, DateUtil.utcNow(), 1));
    }
}
