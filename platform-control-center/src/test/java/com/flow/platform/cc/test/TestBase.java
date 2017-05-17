package com.flow.platform.cc.test;

import com.flow.platform.util.zk.ZkLocalBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
public abstract class TestBase {

    private static ServerCnxnFactory zkFactory;

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        zkFactory = ZkLocalBuilder.start();
    }

    @AfterClass
    public static void done() throws KeeperException, InterruptedException {
        zkFactory.closeAll();
        zkFactory.shutdown();
    }
}
