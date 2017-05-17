package com.flow.platform.cc.test;

import com.flow.platform.cc.service.ZkService;
import com.flow.platform.util.zk.ZkNodeHelper;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
public class ZkServiceTest extends TestBase {

    private final static String TEST_ZONE_NAME = "test-zone";
    private final static String TEST_AGENT_NAME = "test-agent-001";

    @Autowired
    private ZkService zkService;

    @Value("${zk.host}")
    private String zkHost;

    @Value("${zk.timeout}")
    private Integer zkTimeout;

    private ZooKeeper zkClient;

    @Before
    public void before() throws IOException {
        zkClient = new ZooKeeper(zkHost, zkTimeout, null);
    }

    @Test
    public void should_zk_service_initialized() {
        String zonePath = "/flow-agents/" + TEST_ZONE_NAME;
        Assert.assertTrue(ZkNodeHelper.exist(zkClient, zonePath) != null);
    }
}
