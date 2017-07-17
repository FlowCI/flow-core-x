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

package com.flow.platform.cc.test.service;

import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Zone;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gy@fir.im
 */
public class ZoneServiceTest extends TestBase {

    @Autowired
    private ZoneService zoneService;

    @Test
    public void should_create_and_get_zones() {
        // when: create zone;
        String path1 = zoneService.createZone(new Zone("my-test-zone-1", "mock-provider-name"));
        Assert.assertNotNull(path1);
        Assert.assertNotNull("/flow-agents/my-test-zone-1", path1);

        String path2 = zoneService.createZone(new Zone("my-test-zone-2", "mock-provider-name"));
        Assert.assertNotNull(path2);
        Assert.assertNotNull("/flow-agents/my-test-zone-2", path2);

        // then:
        List<Zone> zones = zoneService.getZones();
        Assert.assertNotNull(zones);
        Assert.assertTrue(zones.size() >= 4); // 2 for default, 2 for created
    }
}
