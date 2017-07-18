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

package com.flow.platform.domain.test;

import com.flow.platform.domain.Agent;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gy@fir.im
 */
public class JsonableTest {

    @Test
    public void should_to_json_all_fields() {
        // given:
        Agent agent = new Agent("zone", "name");

        String jsonString = agent.toJson();
        Assert.assertNotNull(jsonString);

        Agent fromJson = Agent.parse(jsonString, Agent.class);
        Assert.assertNotNull(fromJson);
    }
}
