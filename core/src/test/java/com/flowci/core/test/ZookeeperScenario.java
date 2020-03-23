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

package com.flowci.core.test;

import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.ObjectWrapper;
import com.flowci.zookeeper.ZookeeperClient;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@Log4j2
public abstract class ZookeeperScenario extends SpringScenario {

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private ConfigProperties.Zookeeper zkProperties;

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void cleanZkNodes() {
        String root = zkProperties.getAgentRoot();
        for (String child : zk.children(root)) {
            zk.delete(root + "/" + child, true);
        }
    }

    protected Agent mockAgentOnline(String agentPath) throws InterruptedException {
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Agent> wrapper = new ObjectWrapper<>();
        addEventListener(new AgentStatusChangeListener(counter, wrapper));

        zk.create(CreateMode.EPHEMERAL, agentPath, Status.IDLE.getBytes());
        counter.await(10, TimeUnit.SECONDS);

        Assert.assertNotNull(wrapper.getValue());
        Assert.assertEquals(Status.IDLE, wrapper.getValue().getStatus());
        Assert.assertTrue(zk.exist(agentPath + "-lock"));

        return wrapper.getValue();
    }

    protected Agent mockAgentOffline(String agentPath) throws InterruptedException {
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Agent> wrapper = new ObjectWrapper<>();
        addEventListener(new AgentStatusChangeListener(counter, wrapper));

        zk.delete(agentPath, true);
        counter.await(10, TimeUnit.SECONDS);

        Assert.assertNotNull(wrapper.getValue());
        Assert.assertEquals(Status.OFFLINE, wrapper.getValue().getStatus());
        return wrapper.getValue();
    }

    protected Status getAgentStatus(String agentPath) {
        return Status.fromBytes(zk.get(agentPath));
    }

    private static class AgentStatusChangeListener implements ApplicationListener<AgentStatusEvent> {

        public final CountDownLatch counter;

        private final ObjectWrapper<Agent> wrapper;

        private AgentStatusChangeListener(CountDownLatch counter, ObjectWrapper<Agent> wrapper) {
            this.counter = counter;
            this.wrapper = wrapper;
        }

        @Override
        public void onApplicationEvent(AgentStatusEvent event) {
            wrapper.setValue(event.getAgent());
            counter.countDown();
        }
    }
}
