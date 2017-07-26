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

package com.flow.platform.agent.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.agent.LogEventHandler;
import com.flow.platform.cmd.Log;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdType;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author gy@fir.im
 */
public class LogEventHandlerTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Test
    public void should_get_correct_format_websocket() throws Throwable {
        // given:
        Cmd cmd = new Cmd("TestZone", "TestAgent", CmdType.RUN_SHELL, "hello");
        cmd.setId(UUID.randomUUID().toString());
        LogEventHandler logEventHandler = new LogEventHandler(cmd);

        // when:
        String mockLogContent = "hello";
        String socketIoData = logEventHandler.websocketLogFormat(new Log(Log.Type.STDOUT, mockLogContent));

        // then:
        String expect = String
            .format("%s#%s#%s#%s", cmd.getZoneName(), cmd.getAgentName(), cmd.getId(), mockLogContent);
        Assert.assertEquals(expect, socketIoData);
    }

    @Test
    public void should_send_log_to_mq() throws Throwable {
        // given: init rabbitmq
        String url = "amqp://localhost:5672";
        String queueName = "flow-logging-queue-test-for-logging";
        CmdManager.getInstance().kill();

        Config.AGENT_SETTINGS = new AgentSettings(
            url,
            queueName,
            "http://localhost:8080/cmd/report",
            "http://localhost:8080/cmd/log/upload");

        stubFor(post(urlEqualTo("/cmd/report")).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo("/cmd/log/upload")).willReturn(aResponse().withStatus(200)));

        // create queue
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(url);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, true, null);

        // check queue size and content
        final CountDownLatch latch = new CountDownLatch(2);
        channel.basicConsume(queueName, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String consumerTag, Envelope envelope, AMQP.BasicProperties prop, byte[] body) throws IOException {
                String log = new String(body);
                System.out.println("========: " + log);
                Assert.assertNotNull(log);
                latch.countDown();
            }
        });

        // create test cmd which will send two lines of log
        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, "echo hello && echo hello again");
        cmd.setId(UUID.randomUUID().toString());

        // when: execute
        CmdManager.getInstance().execute(cmd);

        // then: check count size should be 0
        latch.await(30, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        channel.queueDelete(queueName);
    }
}
