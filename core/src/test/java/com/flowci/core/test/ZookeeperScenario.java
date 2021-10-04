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

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.Agent.Status;
import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.OnConnectedEvent;
import com.flowci.core.common.config.AppProperties;
import com.flowci.domain.Common;
import com.flowci.domain.ObjectWrapper;
import com.flowci.zookeeper.ZookeeperClient;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@Log4j2
public abstract class ZookeeperScenario extends SpringScenario {

    private final HttpHeaders headers = new HttpHeaders();

    private final Map<String, Object> attributes = new HashMap<>();

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void cleanZkNodes() {
        String root = zkProperties.getAgentRoot();
        for (String child : zk.children(root)) {
            zk.delete(root + "/" + child, true);
        }
    }

    protected Agent mockAgentOnline(String token) throws InterruptedException {
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Agent> wrapper = new ObjectWrapper<>();
        addEventListener(new AgentStatusChangeListener(counter, wrapper));

        AgentInit init = new AgentInit();
        init.setOs(Common.OS.LINUX);
        init.setStatus(Status.IDLE);
        init.setIsDocker(false);
        init.setIsK8sCluster(false);
        init.setPort(2222);

        StandardWebSocketSession session = new StandardWebSocketSession(headers, attributes, null, null, null);
        multicastEvent(new OnConnectedEvent(this, token, session, init, false));

        counter.await(10, TimeUnit.SECONDS);

        Assert.assertNotNull(wrapper.getValue());
        Assert.assertEquals(Status.IDLE, wrapper.getValue().getStatus());

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
