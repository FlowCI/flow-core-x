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
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.queue.PlatformQueue;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class SyncServiceTest extends TestBase {

    @Autowired
    private SyncService syncService;

    @Autowired
    private Path gitWorkspace;

    private List<AgentPath> agents;

    @Before
    public void init() {
        agents = ImmutableList.of(new AgentPath("default", "first"), new AgentPath("default", "second"));
    }

    @Test
    public void should_sync_event_been_added_for_agent() throws Throwable {
        // given:
        AgentPath firstAgent = agents.get(0);
        AgentPath secondAgent = agents.get(1);

        syncService.register(firstAgent);
        syncService.register(secondAgent);

        // when: put sync event to service
        syncService.put(new SyncEvent("http://127.0.0.1/git/hello.git", "v1.0", SyncType.CREATE));
        syncService.put(new SyncEvent("http://127.0.0.1/git/flow.git", "v1.0", SyncType.CREATE));

        // then: two events should be in the agent sync queue
        Assert.assertEquals(2, syncService.get(firstAgent).size());
        Assert.assertEquals(2, syncService.get(secondAgent).size());
    }

    @Test
    public void should_init_sync_event_when_agent_registered() throws Throwable {
        // given: copy exit git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // when: register agent to sync service
        syncService.register(agents.get(0));
        syncService.register(agents.get(1));

        // then: verify the sync event been initialized into both agents
        PlatformQueue<PriorityMessage> queue = syncService.get(agents.get(0));
        SyncEvent event = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", event.getGitUrl());
        Assert.assertEquals("v1.0", event.getTag());
        Assert.assertEquals(SyncType.CREATE, event.getSyncType());

        queue = syncService.get(agents.get(1));
        event = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", event.getGitUrl());
        Assert.assertEquals("v1.0", event.getTag());
        Assert.assertEquals(SyncType.CREATE, event.getSyncType());
    }

    @After
    public void clean() throws IOException {
        File[] files = gitWorkspace.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                FileUtils.forceDelete(file);
            }
        }

        syncService.clean();
    }
}
