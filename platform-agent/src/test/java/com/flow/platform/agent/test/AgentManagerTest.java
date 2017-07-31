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

import com.flow.platform.agent.AgentManager;
import com.flow.platform.agent.Config;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.zk.ZkClient;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgentManagerTest extends TestBase {

    private static final String ZK_ROOT = "/flow-agents";
    private static final String ZONE = "ali";
    private static final String MACHINE = "f-cont-f11f827bd8af1";

    private static TestingServer server;

    private ZkClient zkClient;

    @BeforeClass
    public static void init() throws Throwable {
        System.setProperty(Config.PROP_ENABLE_REALTIME_AGENT_LOG, "false");
        System.setProperty(Config.PROP_UPLOAD_AGENT_LOG, "false");
        System.setProperty(Config.PROP_REPORT_STATUS, "false");

        server = new TestingServer();
        server.start();
    }

    @Before
    public void beforeEach() {
        zkClient = new ZkClient(server.getConnectString(), 1000, 1);
        zkClient.start();

        zkClient.create(ZK_ROOT, null);
        zkClient.create(ZKPaths.makePath(ZK_ROOT, ZONE), null);
    }

    @Test
    public void should_agent_registered() throws Throwable {
        // when: start agent in thread
        AgentManager agent = new AgentManager(server.getConnectString(), 20000, ZONE, MACHINE);
        new Thread(agent).start();
        Thread.sleep(5000); // wait for agent registration

        // when:
        String agentNodePath = ZKPaths.makePath(ZK_ROOT, ZONE, MACHINE);
        Assert.assertEquals(true, zkClient.exist(agentNodePath));
        agent.stop();
    }

    @Test
    public void should_receive_command() throws Throwable {
        AgentManager agent = new AgentManager(server.getConnectString(), 20000, ZONE, MACHINE);
        new Thread(agent).start();
        Thread.sleep(5000); // waitting for node created

        // when: send command to agent
        Cmd cmd = new Cmd(ZONE, MACHINE, CmdType.RUN_SHELL, "echo hello");
        cmd.setId("mock-cmd-id");
        zkClient.setData(agent.getNodePath(), cmd.toBytes());
        Thread.sleep(2000); // waitting for cmd recieved

        // then: check agent status when command received
        Assert.assertEquals(1, agent.getCmdHistory().size());
        Assert.assertEquals(cmd, agent.getCmdHistory().get(0));
        agent.stop();
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
