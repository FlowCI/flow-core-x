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
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.tree.Selector;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    public void should_init_root_node() {
        Assert.assertTrue(zk.exist(zkProperties.getAgentRoot()));
    }

    @Test
    public void should_find_agents_by_tag() {
        agentService.create("agent-1", Sets.newHashSet("local", "k8s"), Optional.empty());
        agentService.create("agent-2", Sets.newHashSet("linux", "k8s"), Optional.empty());
        agentService.create("agent-3", Sets.newHashSet("win"), Optional.empty());
        agentService.create("agent-4", Sets.newHashSet(), Optional.empty());

        // find all agents
        List<Agent> list = agentService.find(new Selector());
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("agent-4", list.get(0).getName());

        // find all k8s tag
        list = agentService.find(new Selector("k8s"));
        Assert.assertEquals(2, list.size());

        // tags with k8s OR win
        list = agentService.find(new Selector("k8s", "win"));
        Assert.assertEquals(3, list.size());

        // tag on win
        list = agentService.find(new Selector("win"));
        Assert.assertEquals(1, list.size());

        // tag on local OR linux
        list = agentService.find(new Selector("local", "linux"));
        Assert.assertEquals(2, list.size());
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
        Agent online = mockAgentOnline(agent.getToken());

        // then:
        Assert.assertEquals(agent, online);
        Assert.assertEquals(Status.IDLE, online.getStatus());
    }

    @Test
    public void should_dispatch_cmd_to_agent() throws InterruptedException {
        // init:
        CmdIn cmd = new ShellIn();
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
