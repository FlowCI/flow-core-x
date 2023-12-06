/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.agent;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.Agent.Status;
import com.flowci.core.agent.domain.AgentOption;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class AgentServiceTest extends ZookeeperScenario {

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentService agentService;

    @Test
    void should_init_root_node() {
        assertTrue(zk.exist(zkProperties.getAgentRoot()));
    }

    @Test
    void should_create_agent_in_db() {
        Agent agent = agentService.create(new AgentOption()
                .setName("hello.test")
                .setTags(ImmutableSet.of("local", "android"))
        );
        assertNotNull(agent);
        assertEquals(agent, agentService.get(agent.getId()));
    }

    @Test
    void should_make_agent_online() throws InterruptedException {
        // init:
        Agent agent = agentService.create(new AgentOption()
                .setName("hello.test")
                .setTags(ImmutableSet.of("local", "android"))
        );

        // when:
        Agent online = mockAgentOnline(agent.getToken());

        // then:
        assertEquals(agent, online);
        assertEquals(Status.IDLE, online.getStatus());
    }

    @Test
    void should_dispatch_cmd_to_agent() throws InterruptedException {
        // init:
        CmdIn cmd = new ShellIn();
        Agent agent = agentService.create(new AgentOption().setName("hello.agent"));

        // when:
        CountDownLatch counter = new CountDownLatch(1);
        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            counter.countDown();
        });

        agentService.dispatch(cmd, agent);

        // then:
        counter.await(10, TimeUnit.SECONDS);
        assertEquals(0, counter.getCount());
    }
}
