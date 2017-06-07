package com.flow.platform.cc.test.cloud;

import com.flow.platform.cc.cloud.MosInstanceManager;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.util.mos.Instance;
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
    private MosInstanceManager mosPoolManager;

    @Ignore
    @Test
    public void should_batch_start_instance() throws Exception {
        List<String> nameList = mosPoolManager.batchStartInstance(POOL_SIZE);
        Assert.assertEquals(POOL_SIZE, nameList.size());

        Thread.sleep(30 * 1000); // wait for instance start

        Collection<Instance> running = mosPoolManager.runningInstance();
        Assert.assertTrue(running.size() >= POOL_SIZE);
    }

    @After
    public void after() {
        mosPoolManager.cleanAll();
    }
}
