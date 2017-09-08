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

package com.flow.platform.cc.test;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.dao.CmdResultDao;
import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.zk.ZKClient;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author gy@fir.im
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class}, initializers = {TestBase.MockContextInitializer.class})
public abstract class TestBase {

    static {
        try {
            System.setProperty("flow.cc.env", "local");
            System.setProperty("flow.cc.task.keep_idle_agent", "false");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static class MockContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            try {
                applicationContext
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(new ResourcePropertySource(new PropertyResourceLoader().find()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    protected Gson gsonConfig;

    @Autowired
    protected AgentDao agentDao;

    @Autowired
    protected CmdDao cmdDao;

    @Autowired
    protected CmdResultDao cmdResultDao;

    @Autowired
    protected ZKClient zkClient;

    protected MockMvc mockMvc;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    @After
    public void afterEach() {
        agentDao.deleteAll();
        cmdDao.deleteAll();
        cmdResultDao.deleteAll();
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
        zkClient.create(ZKHelper.buildPath(agentPath), null);
        return agentPath;
    }

    protected void cleanZookeeperChildrenNode(String node) {
        if (zkClient.exist(node)) {
            return;
        }
        zkClient.delete(node, true);
    }
}
