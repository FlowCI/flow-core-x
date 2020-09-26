package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.Agent;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

public class AgentDaoTest extends SpringScenario {

    @Autowired
    private AgentDao agentDao;

    @Test
    public void should_update_all_status() {
        Agent agent1 = new Agent();
        agent1.setName("hello");
        agent1.setToken("123");
        agentDao.insert(agent1);

        Agent agent2 = new Agent();
        agent2.setName("world");
        agent2.setToken("456");
        agentDao.insert(agent2);

        Assert.assertEquals(Agent.Status.OFFLINE, agent1.getStatus());
        Assert.assertEquals(Agent.Status.OFFLINE, agent2.getStatus());

        agentDao.updateAllStatus(Agent.Status.BUSY);
        Assert.assertEquals(Agent.Status.BUSY, agentDao.findById(agent1.getId()).get().getStatus());
        Assert.assertEquals(Agent.Status.BUSY, agentDao.findById(agent2.getId()).get().getStatus());
    }
}
