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

import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.CmdIn;
import com.flowci.domain.CmdType;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class AgentServiceTest extends ZookeeperScenario {

    @Autowired
    private ConfigProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentService agentService;

    @Test
    public void should_init_root_node() {
        Assert.assertTrue(zk.exist(zkProperties.getAgentRoot()));
    }

    @Test
    public void should_create_agent_in_db() {
        Agent agent = agentService.create("hello.test", ImmutableSet.of("local", "android"), Optional.empty());
        Assert.assertNotNull(agent);
        Assert.assertEquals(agent, agentService.get(agent.getId()));
    }

    @Test
    public void should_make_agent_online() throws InterruptedException {
        // init:
        Agent agent = agentService.create("hello.test", ImmutableSet.of("local", "android"), Optional.empty());

        // when:
        Agent online = mockAgentOnline(agentService.getPath(agent));

        // then:
        Assert.assertEquals(agent, online);
        Assert.assertEquals(Status.IDLE, online.getStatus());
    }

    @Test
    public void should_find_lock_and_release_agents() throws InterruptedException {
        // init:
        agentService.create("hello.test.1", ImmutableSet.of("local", "android"), Optional.empty());
        agentService.create("hello.test.2", null, Optional.empty());
        Agent idle = agentService.create("hello.test.3", ImmutableSet.of("alicloud", "android"), Optional.empty());
        ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(5, 5, 0, "mock-tryLock-");

        // when:
        Agent agent = agentService.find(Status.CREATED, ImmutableSet.of("android")).get(0);
        Assert.assertNotNull(agent);

        // when: make agent online
        mockAgentOnline(agentService.getPath(idle));

        // then: find available agent
        Agent available = agentService.find(Status.IDLE, null).get(0);
        Assert.assertEquals(idle, available);

        // when: try lock agent in multiple thread
        AtomicInteger numOfLocked = new AtomicInteger(0);
        AtomicInteger numOfFailure = new AtomicInteger(0);
        CountDownLatch counterForLock = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                Boolean isLocked = agentService.tryLock("dummyJobId", available.getId());
                if (isLocked) {
                    numOfLocked.incrementAndGet();
                }
                else {
                    numOfFailure.incrementAndGet();
                }

                counterForLock.countDown();
            });
        }

        // then: verify num of locked
        counterForLock.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(1, numOfLocked.get());
        Assert.assertEquals(4, numOfFailure.get());
        Assert.assertEquals(Status.BUSY, getAgentStatus(agentService.getPath(available)));

        // when: release agent and mock event from agent
        agentService.tryRelease(available.getId());
//        mockReleaseAgent(agentService.getPath(available));

        // then: the status should be idle
        Status statusFromZk = getAgentStatus(agentService.getPath(available));
        Assert.assertEquals(Status.IDLE, statusFromZk);

        ThreadHelper.sleep(2000);
        Status statusFromDB = agentService.get(available.getId()).getStatus();
        Assert.assertEquals(Status.IDLE, statusFromDB);
    }

    @Test
    public void should_dispatch_cmd_to_agent() throws InterruptedException {
        // init:
        CmdIn cmd = new CmdIn(UUID.randomUUID().toString(), CmdType.SHELL);
        Agent agent = agentService.create("hello.agent", null, Optional.empty());

        // when:
        CountDownLatch counter = new CountDownLatch(1);
        addEventListener((ApplicationListener<CmdSentEvent>) event -> {
            counter.countDown();
        });

        agentService.dispatch(cmd, agent);

        // then:
        counter.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, counter.getCount());
    }
}
