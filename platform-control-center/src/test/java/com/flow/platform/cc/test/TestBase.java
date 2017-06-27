package com.flow.platform.cc.test;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.cc.util.ZkHelper;
import com.flow.platform.dao.AgentDao;
import com.flow.platform.dao.CmdDao;
import com.flow.platform.dao.CmdResultDao;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.zk.ZkLocalBuilder;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.gson.Gson;
import org.apache.zookeeper.ZooKeeper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class})
public abstract class TestBase {

    static {
        try {
            System.setProperty("flow.cc.env", "local");
            System.setProperty("flow.cc.task.keep_idle_agent", "false");

            ZkLocalBuilder.start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    protected ZkHelper zkHelper;

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    protected Gson gsonConfig;

    protected ZooKeeper zkClient;

    protected MockMvc mockMvc;

    @Autowired
    protected AgentDao agentDao;

    @Autowired
    protected CmdDao cmdDao;

    @Autowired
    protected CmdResultDao cmdResultDao;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
        zkClient = zkHelper.getClient();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        // clean up cmd log folder
        Stream<Path> list = Files.list(AppConfig.CMD_LOG_DIR);
        list.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected AgentPath createMockAgent(String zone, String agent) {
        AgentPath agentPath = new AgentPath(zone, agent);
        ZkNodeHelper.createEphemeralNode(zkClient, zkHelper.getZkPath(agentPath), "");
        return agentPath;
    }
}
