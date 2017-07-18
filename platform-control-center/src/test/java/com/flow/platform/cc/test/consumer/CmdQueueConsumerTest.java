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

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.*;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.rabbitmq.client.Channel;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class CmdQueueConsumerTest extends TestBase {

    private final static String ZONE = "ut-test-zone-for-queue";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8088);

    @Autowired
    private Channel cmdSendChannel;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Value("${mq.exchange.name}")
    private String cmdExchangeName;

    @Before
    public void before() {
        zoneService.createZone(new Zone(ZONE, "mock-cloud-provider"));
    }

    @Test
    public void should_receive_cmd_from_queue() throws Throwable {
        // given:
        String agentName = "mock-agent-for-queue-test";
        AgentPath agentPath = createMockAgent(ZONE, agentName);
        Thread.sleep(2000);

        Agent agent = agentService.find(agentPath);
        Assert.assertNotNull(agent);
        Assert.assertEquals(AgentStatus.IDLE, agent.getStatus());

        // mock callback url
        stubFor(post(urlEqualTo("/node/callback")).willReturn(aResponse().withStatus(200)));

        // when: send cmd by rabbit mq with cmd exchange name
        CmdInfo mockCmd = new CmdInfo(ZONE, agentName, CmdType.RUN_SHELL, "echo hello");
        mockCmd.setWebhook("http://localhost:8088/node/callback");
        cmdSendChannel.basicPublish(cmdExchangeName, "", null, mockCmd.toBytes());
        Thread.sleep(2000);

        // then: webhook been invoked
        verify(1, postRequestedFor(urlEqualTo("/node/callback")));

        // then: cmd should received in zookeeper agent node
        byte[] raw = zkClient.getData(zkHelper.getZkPath(agentPath), false, null);
        Cmd received = Cmd.parse(raw, Cmd.class);
        Assert.assertNotNull(received);
        Assert.assertNotNull(received.getId());
        Assert.assertEquals(mockCmd.getAgentPath(), received.getAgentPath());
    }

    @Ignore("this ut not finished yet")
    @Test
    public void should_re_enqueue_if_no_agent() throws Throwable {
        // when: send cmd without available agent
        CmdInfo mockCmd = new CmdInfo(ZONE, null, CmdType.RUN_SHELL, "echo hello");
        cmdSendChannel.basicPublish(cmdExchangeName, "", null, mockCmd.toBytes());
        Thread.sleep(30000); // wait for re_enqueue
    }

    private static HttpResponse httpSend(final HttpUriRequest request) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return client.execute(request);
        }
    }
}
