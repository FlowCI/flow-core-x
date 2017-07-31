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

import com.flow.platform.agent.Config;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.util.zk.ZkClient;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * @author gy@fir.im
 */
public class ConfigTest extends TestBase {

    private final static String ROOT_PATH = "/flow-agents";

    private static TestingServer server;

    private ZkClient zkClient;

    @BeforeClass
    public static void init() throws Throwable {
        server = new TestingServer();
        server.start();
    }

    @Before
    public void before() throws Throwable {
        zkClient = new ZkClient(server.getConnectString());
        zkClient.start();

        String path = zkClient.create(ROOT_PATH, null);
        Assert.assertEquals(ROOT_PATH, path);
        Assert.assertEquals(true, zkClient.exist(path));
    }

    @Test
    public void should_load_agent_config() throws IOException, InterruptedException {
        // given:
        String loggingQueueHost = "amqp://127.0.0.1:5672";
        String loggingQueueName = "flow-logging-queue-test";
        String cmdStatusUrl = "http://localhost:8080/cmd/report";
        String cmdLogUrl = "http://localhost:8080/cmd/log/upload";
        AgentSettings config = new AgentSettings(loggingQueueHost, loggingQueueName, cmdStatusUrl, cmdLogUrl);

        // when: create zone with agent config
        String zonePath = ZKPaths.makePath(ROOT_PATH, "ali");
        zkClient.createEphemeral(zonePath, config.toBytes());

        // then:
        Config.AGENT_SETTINGS = Config.loadAgentConfig(server.getConnectString(), "ali", 5);
        Assert.assertNotNull(Config.agentSettings());
        Assert.assertEquals(loggingQueueHost, Config.agentSettings().getLoggingQueueHost());
        Assert.assertEquals(loggingQueueName, Config.agentSettings().getLoggingQueueName());
        Assert.assertEquals(cmdStatusUrl, Config.agentSettings().getCmdStatusUrl());
        Assert.assertEquals(cmdLogUrl, Config.agentSettings().getCmdLogUrl());
    }

    @After
    public void after() throws Throwable {
        zkClient.close();
    }

    @AfterClass
    public static void done() throws Throwable {
        server.stop();
    }
}
