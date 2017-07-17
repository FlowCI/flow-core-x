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

package com.flow.platform.cc.test.util;

import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Zone;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author gy@fir.im
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
