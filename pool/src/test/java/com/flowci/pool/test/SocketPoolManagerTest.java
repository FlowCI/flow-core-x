/*
 * Copyright 2020 flow.ci
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

package com.flowci.pool.test;

import java.util.Optional;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.DockerPoolException;
import com.flowci.pool.manager.PoolManager;
import com.flowci.pool.manager.SocketPoolManager;
import com.flowci.util.StringHelper;

import org.junit.*;

public class SocketPoolManagerTest extends PoolScenario {

    private final PoolManager<SocketInitContext> manager = new SocketPoolManager();

    @Before
    public void init() throws Exception {
        manager.init(new SocketInitContext());
    }

    @After
    public void cleanUp() throws DockerPoolException {
        for (AgentContainer c : manager.list(Optional.empty())) {
            manager.remove(c.getAgentName());
        }
    }

    @Test
    public void should_start_agent_and_stop() throws Exception {
        final String name = StringHelper.randomString(5);

        StartContext context = new StartContext();
        context.setAgentName(name);
        context.setServerUrl("http://localhost:8080");
        context.setToken("helloworld");

        manager.start(context);
        Assert.assertEquals(DockerStatus.Running, manager.status(name));
        Assert.assertEquals(1, manager.list(Optional.empty()).size());
        Assert.assertEquals(1, manager.size());

        manager.stop(name);
        Assert.assertEquals(DockerStatus.Exited, manager.status(name));
        Assert.assertEquals(1, manager.list(Optional.empty()).size());
        Assert.assertEquals(1, manager.size());

        manager.remove(name);
        Assert.assertEquals(DockerStatus.None, manager.status(name));
        Assert.assertEquals(0, manager.list(Optional.empty()).size());
        Assert.assertEquals(0, manager.size());
    }
}