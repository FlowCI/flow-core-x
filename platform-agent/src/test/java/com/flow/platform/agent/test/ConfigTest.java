package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.util.zk.ZkLocalBuilder;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public class ConfigTest {

    private static ZooKeeper zkClient;
    private static ServerCnxnFactory zkFactory;

    @BeforeClass
    public static void init() throws IOException, InterruptedException, KeeperException {
        zkFactory = ZkLocalBuilder.start();
        zkClient = new ZooKeeper("localhost:2181", 20000, null);
        ZkNodeHelper.createNode(zkClient, "/flow-agents", ""); // create root node
    }

    @Test
    public void should_load_agent_config() throws IOException, InterruptedException {
        // given:
        String loggingUrl = "http://localhost:3000/agent";
        String statusUrl = "http://localhost:8080/agent/status";
        AgentConfig config = new AgentConfig(loggingUrl, statusUrl);

        // when: create zone with agent config
        String zonePath = "/flow-agents/ali";
        ZkNodeHelper.createNode(zkClient, zonePath, config.toJson().getBytes()); // create zone node
        Thread.sleep(500);

        // then:
        Config.AGENT_CONFIG = Config.loadAgentConfig("localhost:2181", 20000, "ali", 5);
        Assert.assertNotNull(Config.agentConfig());
        Assert.assertEquals(loggingUrl, Config.agentConfig().getLoggingUrl());
        Assert.assertEquals(statusUrl, Config.agentConfig().getStatusUrl());
    }

    @AfterClass
    public static void done() throws KeeperException, InterruptedException {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }
}
