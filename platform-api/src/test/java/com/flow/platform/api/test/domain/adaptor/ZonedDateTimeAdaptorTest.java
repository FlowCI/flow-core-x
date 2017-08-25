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

package com.flow.platform.api.test.domain.adaptor;

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */

public class ZonedDateTimeAdaptorTest extends TestBase {

    @Test
    public void should_equal_serializer_success_time_not_null() {
        Flow flow = new Flow("/flow", "flow");
        flow.setCreatedAt(ZonedDateTime.now());
        flow.setUpdatedAt(ZonedDateTime.now());
        String json = flow.toJson();
        Flow f = Jsonable.GSON_CONFIG.fromJson(json, Flow.class);
        Assert.assertEquals(flow.getUpdatedAt().toEpochSecond(), f.getUpdatedAt().toEpochSecond());
        Assert.assertEquals(flow.getCreatedAt().toEpochSecond(), f.getCreatedAt().toEpochSecond());
    }

    @Test
    public void should_equal_serializer_success_time_is_null() {
        Flow flow = new Flow("/flow", "flow");
        String json = flow.toJson();
        Flow f = Jsonable.GSON_CONFIG.fromJson(json, Flow.class);
        Assert.assertNull(flow.getCreatedAt());
        Assert.assertNull(flow.getUpdatedAt());
        Assert.assertNull(f.getUpdatedAt());
        Assert.assertNull(f.getCreatedAt());
    }
}
