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

import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author gy@fir.im
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