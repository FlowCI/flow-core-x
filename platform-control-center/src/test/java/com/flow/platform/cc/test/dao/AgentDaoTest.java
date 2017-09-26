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

package com.flow.platform.cc.test.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentStatus;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author gy@fir.im
 */
public class AgentDaoTest extends TestBase {

    @Test(expected = DuplicateKeyException.class)
    @Transactional
    public void should_raise_exception_when_create_duplicate_agent() throws Throwable {
        Agent agent1 = new Agent("zone-1", "agent-1");
        agent1.setStatus(AgentStatus.OFFLINE);
        agentDao.save(agent1);

        agent1 = new Agent("zone-1", "agent-1");
        agent1.setStatus(AgentStatus.OFFLINE);
        agentDao.save(agent1);
    }

    @Test
    @Transactional
    public void should_list_agent_by_zone_and_status() throws Throwable {
        // given:
        final String zone1 = "zone-1";
        final String zone2 = "zone-2";

        Agent agent1 = new Agent(zone1, "agent-1");
        agent1.setStatus(AgentStatus.OFFLINE);
        agentDao.save(agent1);

        Agent agent2 = new Agent(zone1, "agent-2");
        agent2.setStatus(AgentStatus.BUSY);
        agentDao.save(agent2);

        Agent agent3 = new Agent(zone2, "agent-3");
        agent3.setStatus(AgentStatus.IDLE);
        agentDao.save(agent3);

        List<Agent> list = agentDao.list(Sets.newHashSet(agent1.getPath(), agent2.getPath(), agent3.getPath()));
        Assert.assertTrue(list.size() == 3);

        // when: find agents of zone 1 without status
        List<Agent> agents = agentDao.list(zone1, null);
        Assert.assertNotNull(agents);
        Assert.assertEquals(2, agents.size());

        // when: find agents by zone 1 with idle status
        agents = agentDao.list(zone1, null, AgentStatus.IDLE);
        Assert.assertNotNull(agents);
        Assert.assertEquals(0, agents.size());

        // when: find agents of zone 2 without status
        agents = agentDao.list(zone2, null);
        Assert.assertNotNull(agents);
        Assert.assertEquals(1, agents.size());
        Assert.assertEquals(agent3, agents.get(0));

        // when: find empty zone list
        agents = agentDao.list("empty-zone", null);
        Assert.assertNotNull(agents);
        Assert.assertEquals(0, agents.size());
    }

    @Test
    public void should_batch_update_agent_status() throws Throwable {
        // given:
        final String zone = "zone-2";

        Agent agent1 = new Agent(zone, "agent-1");
        agent1.setStatus(AgentStatus.OFFLINE);
        agent1.setWebhook("http://test.com/agent/callback");
        agentDao.save(agent1);

        Agent agent2 = new Agent(zone, "agent-2");
        agent2.setStatus(AgentStatus.BUSY);
        agentDao.save(agent2);

        Agent agent3 = new Agent(zone, "agent-3");
        agent3.setStatus(AgentStatus.BUSY);
        agentDao.save(agent3);

        // when: batch update status
        int numOfUpdate = agentDao
            .batchUpdateStatus(zone, AgentStatus.IDLE, Sets.newHashSet("agent-1", "agent-2"), false);

        // then:
        Assert.assertEquals(2, numOfUpdate);
        Assert.assertEquals("http://test.com/agent/callback", agentDao.get(agent1.getPath()).getWebhook());
        Assert.assertEquals(AgentStatus.IDLE, agentDao.get(agent1.getPath()).getStatus());
        Assert.assertEquals(AgentStatus.IDLE, agentDao.get(agent2.getPath()).getStatus());
        Assert.assertEquals(AgentStatus.BUSY, agentDao.get(agent3.getPath()).getStatus());

        // when: batch update status by NOT clause
        numOfUpdate = agentDao.batchUpdateStatus(zone, AgentStatus.OFFLINE, Sets.newHashSet("agent-1", "agent-2"), true);

        // then:
        Assert.assertEquals(1, numOfUpdate);
        Assert.assertEquals(AgentStatus.IDLE, agentDao.get(agent1.getPath()).getStatus());
        Assert.assertEquals(AgentStatus.IDLE, agentDao.get(agent2.getPath()).getStatus());
        Assert.assertEquals(AgentStatus.OFFLINE, agentDao.get(agent3.getPath()).getStatus());

    }
}
