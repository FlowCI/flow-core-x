package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.test.SpringScenario;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(Agent.Status.OFFLINE, agent1.getStatus());
        assertEquals(Agent.Status.OFFLINE, agent2.getStatus());

        agentDao.updateAllStatus(Agent.Status.BUSY);
        assertEquals(Agent.Status.BUSY, agentDao.findById(agent1.getId()).get().getStatus());
        assertEquals(Agent.Status.BUSY, agentDao.findById(agent2.getId()).get().getStatus());
    }

    @Test
    public void should_find_agent_by_tags_and_status() {
        Agent agent1 = new Agent();
        agent1.setName("hello");
        agent1.setToken("123");
        agent1.setStatus(Agent.Status.BUSY);
        agentDao.insert(agent1);

        Agent agent2 = new Agent();
        agent2.setName("world");
        agent2.setToken("456");
        agent2.setStatus(Agent.Status.IDLE);
        agentDao.insert(agent2);

        List<Agent> list = agentDao.findAll(Sets.newHashSet(), Sets.newHashSet(Agent.Status.IDLE, Agent.Status.BUSY));
        assertEquals(2, list.size());

        list = agentDao.findAll(Sets.newHashSet(), Sets.newHashSet(Agent.Status.IDLE));
        assertEquals(1, list.size());
    }
}
