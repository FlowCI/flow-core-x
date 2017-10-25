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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.http.HttpURL;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.zookeeper.KeeperException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class AgentServiceTest extends TestBase {

    private final static String MOCK_PROVIDER_NAME = "mock-cloud-provider";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8088);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private List<Zone> defaultZones;

    @Value("${agent.config.ws}")
    private String wsDomain;

    @Value("{agent.config.cc}")
    private String ccDomain;

    @Test
    public void should_create_agent_and_send_webhook_if_agent_status_changed() throws Throwable {
        // given: mock agent webhook callback
        String webhook = "http://localhost:8088/agent/callback";
        stubFor(post(urlEqualTo("/agent/callback")).willReturn(aResponse().withStatus(200)));

        // when:
        AgentPath agentPath = new AgentPath("default", "hello-agent");
        agentService.create(agentPath, webhook);

        // then:
        Agent loaded = agentService.find(agentPath);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(agentPath, loaded.getPath());
        Assert.assertEquals(webhook, loaded.getWebhook());
        Assert.assertEquals(AgentStatus.OFFLINE, loaded.getStatus());

        // when: mock agent status changed
        agentService.saveWithStatus(loaded, AgentStatus.IDLE);
        Thread.sleep(1000); // wait for webhook callback

        // then: check webhook is send or not
        CountMatchingStrategy strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(strategy, postRequestedFor(urlEqualTo("/agent/callback")));
    }

    @Test
    public void should_has_agent_config_in_zone_data() throws Throwable {
        // given:
        String zoneName = "test-zone-00";
        zoneService.createZone(new Zone(zoneName, "test"));
        Thread.sleep(1000);

        // when:
        String zonePath = ZKHelper.buildPath(zoneName, null);
        byte[] raw = zkClient.getData(zonePath);

        // then:
        AgentSettings config = Jsonable.parse(raw, AgentSettings.class);
        Assert.assertNotNull(config);

        final String cmdWebsocketForLog = HttpURL.build(wsDomain).append("/agent/cmd/logging").toString();
        Assert.assertEquals(cmdWebsocketForLog, config.getWebSocketUrl());

        final String cmdReportUrl = HttpURL.build(ccDomain).append("/cmd/report").toString();
        Assert.assertEquals(cmdReportUrl, config.getCmdStatusUrl());

        final String cmdLogUploadUrl = HttpURL.build(ccDomain).append("/cmd/log/upload").toString();
        Assert.assertEquals(cmdLogUploadUrl, config.getCmdLogUrl());
    }

    @Test
    public void should_agent_initialized() throws InterruptedException, KeeperException {
        // given:
        String zoneName = "ut-test-zone-1";
        zoneService.createZone(new Zone(zoneName, MOCK_PROVIDER_NAME));
        Assert.assertEquals(0, agentService.listForOnline(zoneName).size());

        String agentName = "test-agent-001";
        String path = ZKHelper.buildPath(zoneName, agentName);

        // when: simulate to create agent
        zkClient.createEphemeral(path, null);

        // then:
        Thread.sleep(1000);
        Assert.assertEquals(1, agentService.listForOnline(zoneName).size());
        Assert.assertTrue(agentService.listForOnline(zoneName).contains(new Agent(zoneName, agentName)));
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
        zkClient.createEphemeral(ZKHelper.buildPath(agent11), null);


        Thread.sleep(1000); // mock network delay

        AgentPath agent12 = new AgentPath(zone_1, "agent-2");
        zkClient.createEphemeral(ZKHelper.buildPath(agent12), null);

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
        zkClient.createEphemeral(ZKHelper.buildPath(agent13), null);

        Thread.sleep(2000); // waiting for watcher call

        // then: should has three online agent in zone-1
        Assert.assertEquals(3, agentService.listForOnline(zone_1).size());

        // when: agent online to zone-2
        AgentPath agent2 = new AgentPath(zone_2, "agent-1");
        zkClient.createEphemeral(ZKHelper.buildPath(agent2), null);
        Thread.sleep(1000); // waiting for watcher call

        // then: should has one online agent in zone-2
        Assert.assertEquals(1, agentService.listForOnline(zone_2).size());

        // then: still has three online agent in zone-1
        Assert.assertEquals(3, agentService.listForOnline(zone_1).size());

        // when: there is one agent offline in zone-1
        zkClient.delete(ZKHelper.buildPath(agent12), true);
        Thread.sleep(1000); // waiting for watcher call

        // then: should left two agent in zone-1
        Assert.assertEquals(2, agentService.listForOnline(zone_1).size());
        Agent agent11Loaded = (Agent) agentService.listForOnline(zone_1).toArray()[0];
        Assert.assertEquals(agent11, agent11Loaded.getPath());
    }

    @Test
    public void should_report_agent_status() throws InterruptedException {
        // given: init zk agent
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-for-status";
        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);
        Thread.sleep(500);

        // when: report status
        AgentPath pathObj = new AgentPath(zoneName, agentName);
        Agent created = agentService.find(pathObj);
        agentService.saveWithStatus(created, AgentStatus.BUSY);

        // then:
        Agent exit = agentService.find(pathObj);
        Assert.assertEquals(AgentStatus.BUSY, exit.getStatus());
    }

    @Test(expected = AgentErr.NotFoundException.class)
    public void should_raise_not_found_exception_when_report_status() {
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-for-status-exception";

        AgentPath pathObj = new AgentPath(zoneName, agentName);
        Agent notExist = new Agent(pathObj);
        agentService.saveWithStatus(notExist, AgentStatus.BUSY);
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

    @Test
    public void should_delete_agent() throws Exception{
        String zoneName = defaultZones.get(0).getName();
        String agentName = "test-agent-for-status";
        String agentPath = ZKHelper.buildPath(zoneName, agentName);
        zkClient.createEphemeral(agentPath, null);

        Thread.sleep(500);

        // when: report status
        AgentPath pathObj = new AgentPath(zoneName, agentName);
        Agent created = agentService.find(pathObj);
        Assert.assertNotNull(created);
        agentService.saveWithStatus(created, AgentStatus.BUSY);

        agentService.delete(created);
        Assert.assertNull(agentService.find(new AgentPath(zoneName, agentName)));
    }
}
