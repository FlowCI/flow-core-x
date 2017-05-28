package com.flow.platform.cc.test;

import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.util.ZkHelper;
import com.flow.platform.util.zk.ZkLocalBuilder;
import com.google.gson.Gson;
import org.apache.zookeeper.ZooKeeper;
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
            ZkLocalBuilder.start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    protected ZkHelper zkHelper;

    @Autowired
    private WebApplicationContext webAppContext;

    protected ZooKeeper zkClient;

    protected Gson gson = new Gson();

    protected MockMvc mockMvc;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
        zkClient = zkHelper.getClient();
    }
}
