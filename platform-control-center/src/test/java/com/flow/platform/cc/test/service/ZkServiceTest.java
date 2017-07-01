package com.flow.platform.cc.test.service;

import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZkServiceTest extends TestBase {

    @Test
    public void should_zk_service_initialized() {
        for (Zone zone : zkHelper.getDefaultZones()) {
            String zonePath = ZkPathBuilder.create("flow-agents").append(zone.getName()).path();
            Assert.assertTrue(ZkNodeHelper.exist(zkClient, zonePath) != null);
        }
    }
}