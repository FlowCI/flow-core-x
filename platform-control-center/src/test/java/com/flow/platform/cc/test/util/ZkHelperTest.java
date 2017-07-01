package com.flow.platform.cc.test.util;

import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Zone;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by gy@fir.im on 01/07/2017.
 * Copyright fir.im
 */
public class ZkHelperTest extends TestBase {

    @Test
    public void should_load_default_zone_definition_from_config() throws Throwable {
        List<Zone> zones = zkHelper.getDefaultZones();
        Assert.assertEquals(3, zones.size());

        Zone mosTestZone = zones.get(2);
        Assert.assertNotNull(mosTestZone.getCloudProvider());
        Assert.assertNotNull(mosTestZone.getImageName());
        Assert.assertNotNull(mosTestZone.getMinPoolSize());
        Assert.assertNotNull(mosTestZone.getMaxPoolSize());
    }
}
