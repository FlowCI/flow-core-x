package com.flow.platform.dao.test;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by gy@fir.im on 24/06/2017.
 * Copyright fir.im
 */
public class AgentDaoTest extends TestBase {

    @Test
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

        // when: find agents of zone 1 without status
        List<Agent> agents = agentDao.list(zone1);
        Assert.assertNotNull(agents);
        Assert.assertEquals(2, agents.size());

        // when: find agents by zone 1 with idle status
        agents = agentDao.list(zone1, AgentStatus.IDLE);
        Assert.assertNotNull(agents);
        Assert.assertEquals(0, agents.size());

        // when: find agents of zone 2 without status
        agents = agentDao.list(zone2);
        Assert.assertNotNull(agents);
        Assert.assertEquals(1, agents.size());
        Assert.assertEquals(agent3, agents.get(0));

        // when: find empty zone list
        agents = agentDao.list("empty-zone");
        Assert.assertNotNull(agents);
        Assert.assertEquals(0, agents.size());
    }
}
