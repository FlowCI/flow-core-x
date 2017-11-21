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

package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.AgentPath;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class SyncServiceTest extends TestBase {

    @Autowired
    private SyncService syncService;

    @Test
    public void should_event_been_added_for_agent() throws Throwable {
        // given:
        AgentPath firstAgent = new AgentPath("default", "first");
        AgentPath secondAgent = new AgentPath("default", "second");

        syncService.register(firstAgent);
        syncService.register(secondAgent);

        // when: put sync event to service
        syncService.put(new SyncEvent("http://127.0.0.1/git/hello.git", "v1.0"));
        syncService.put(new SyncEvent("http://127.0.0.1/git/flow.git", "v1.0"));

        // then: two events should be in the agent sync queue
        Assert.assertEquals(2, syncService.get(firstAgent).size());
        Assert.assertEquals(2, syncService.get(secondAgent).size());
    }
}
