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

package com.flow.platform.cc.test.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class CmdQueueConsumerTest extends TestBase {

    private final static String ZONE = "ut-test-zone-for-queue";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8088);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private CmdService cmdService;

    @Before
    public void before() throws Throwable {
        zoneService.createZone(new Zone(ZONE, "mock-cloud-provider"));
    }

    @Test
    public void should_receive_cmd_from_queue() throws Throwable {
        // given:
        String agentName = "agent-name-test";
        zkClient.delete(ZKHelper.buildPath(ZONE, agentName), false);

        AgentPath agentPath = createMockAgent(ZONE, agentName);
        Thread.sleep(1000);

        Assert.assertTrue(zkClient.exist(ZKHelper.buildPath(agentPath)));

        Agent agent = agentService.find(agentPath);
        Assert.assertNotNull(agent);
        Assert.assertEquals(AgentStatus.IDLE, agent.getStatus());

        // mock callback url
        stubFor(post(urlEqualTo("/node/callback")).willReturn(aResponse().withStatus(200)));

        // when: send cmd by rabbit mq with cmd exchange name
        CmdInfo mockCmd = new CmdInfo(ZONE, agentName, CmdType.RUN_SHELL, "echo hello");
        mockCmd.setWebhook("http://localhost:8088/node/callback");

        Cmd mockCmdInstance = cmdService.queue(mockCmd, 1, 0);
        Assert.assertNotNull(mockCmdInstance.getId());

        Thread.sleep(1000);

        // then: webhook been invoked
        verify(1, postRequestedFor(urlEqualTo("/node/callback")));

        // then: cmd should received in zookeeper agent node
        byte[] raw = zkClient.getData(ZKHelper.buildPath(agentPath));
        Cmd received = Cmd.parse(raw, Cmd.class);
        Assert.assertNotNull(received);
        Assert.assertNotNull(received.getId());
        Assert.assertEquals(mockCmd.getAgentPath(), received.getAgentPath());
    }

    @Test
    public void should_retry_cmd_in_queue() throws Throwable {
        // given:
        String url = "/node/test-for-retry/callback";
        stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(200)));

        // when: send to queue and waiting for retry 3 times
        CmdInfo mockCmd = new CmdInfo(ZONE, null, CmdType.RUN_SHELL, "echo hello");
        mockCmd.setWebhook("http://localhost:8088" + url);

        Cmd cmd = cmdService.queue(mockCmd, 1, 3);
        Assert.assertNotNull(cmdService.find(cmd.getId()));

        Thread.sleep(10000); // wait for retrying.

        // then: check num of request
        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 3);
        verify(countStrategy, postRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void should_re_enqueue_if_no_agent() throws Throwable {
        // given:
        String testUrl = "/node/path-of-node/callback";
        String agentName = "agent-for-retry-queue-test";
        stubFor(post(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(200)));

        // when: send cmd without available agent
        CmdInfo mockCmd = new CmdInfo(ZONE, agentName, CmdType.RUN_SHELL, "echo hello");
        mockCmd.setWebhook("http://localhost:8088" + testUrl);
        Cmd mockCmdInstance = cmdService.queue(mockCmd, 1, 5);
        Assert.assertNotNull(mockCmdInstance.getId());

        // wait for send webhook
        Thread.sleep(500);

        // then: should invoke cmd webhook for status REJECT
        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 1);
        verify(countStrategy, postRequestedFor(urlEqualTo(testUrl)));

        // when:
        createMockAgent(ZONE, agentName);
        Thread.sleep(5000); // wait for enqueue again

        // then:
        countStrategy = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 2);
        verify(countStrategy, postRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    public void should_stop_queued_cmd() throws Throwable {
        // given:
        String testUrl = "/node/path-of-node-for-stop/callback";
        stubFor(post(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(200)));

        // when: send cmd without available agent
        CmdInfo mockCmd = new CmdInfo(ZONE, null, CmdType.RUN_SHELL, "echo hello");
        mockCmd.setWebhook("http://localhost:8088" + testUrl);
        Cmd mockCmdInstance = cmdService.queue(mockCmd, 1, 5);

        Assert.assertNotNull(mockCmdInstance.getId());
        Assert.assertNotNull(cmdDao.get(mockCmdInstance.getId()));

        // wait for send webhook
        Thread.sleep(2000);

        // then: verify has webhook callback if no available agent found
        CountMatchingStrategy countStrategy = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 1);
        verify(countStrategy, postRequestedFor(urlEqualTo(testUrl)));

        // when: set cmd to stop status
        try {
            CmdStatusItem statusItem = new CmdStatusItem(mockCmdInstance.getId(), CmdStatus.STOPPED, null, false, true);
            cmdService.updateStatus(statusItem, false);

            // wait for send webhook
            Thread.sleep(1000);

            // then:
            countStrategy = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 2);
            verify(countStrategy, postRequestedFor(urlEqualTo(testUrl)));
        } catch (CannotAcquireLockException acquireLockException) {
            // may raise the exception when this cmd is processing, in api level should return stop cmd failure
        }
    }

    @After
    public void deleteZone() {
        deleteNodeWithChildren(ZKHelper.buildPath(ZONE, null));
    }
}
