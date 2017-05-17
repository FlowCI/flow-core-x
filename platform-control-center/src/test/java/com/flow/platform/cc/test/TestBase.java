package com.flow.platform.cc.test;

import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.util.zk.ZkLocalBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

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
}
