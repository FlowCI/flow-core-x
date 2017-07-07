package com.flow.platform.cc.test.cloud;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.cloud.MosInstanceManager;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Instance;
import com.flow.platform.domain.Zone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

/**
 * Created by gy@fir.im on 05/06/2017.
 * Copyright fir.im
 */
public class MosInstanceManagerTest extends TestBase {

    private final static int POOL_SIZE = 1;

    @Autowired
    private InstanceManager mosInstanceManager;

    @Ignore
    @Test
    public void should_batch_start_instance() throws Exception {
        // given:
        Zone zone = new Zone("test-zone", "mos");
        zone.setImageName("flow-osx-83-109-bj4-zk-agent");
        zone.setMinPoolSize(1);
        zone.setMaxPoolSize(1);
        zone.setNumOfStart(1);

        List<String> nameList = mosInstanceManager.batchStartInstance(zone);
        Assert.assertEquals(POOL_SIZE, nameList.size());

        Thread.sleep(60 * 1000); // wait for instance start

        Collection<Instance> running = mosInstanceManager.instances();
        Assert.assertTrue(running.size() >= POOL_SIZE);

        Instance mosInstance = mosInstanceManager.find(nameList.get(0));
        Assert.assertNotNull(mosInstance);
    }

    @After
    public void after() {
        mosInstanceManager.cleanAll();
    }
}
