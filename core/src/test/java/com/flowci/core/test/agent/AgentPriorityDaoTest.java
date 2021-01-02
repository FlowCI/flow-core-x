package com.flowci.core.test.agent;

import com.flowci.core.agent.dao.AgentPriorityDao;
import com.flowci.core.agent.domain.AgentPriority;
import com.flowci.core.test.SpringScenario;
import com.flowci.tree.Selector;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class AgentPriorityDaoTest extends SpringScenario {

    @Autowired
    private AgentPriorityDao agentPriorityDao;

    @Test
    public void should_operate_agent_priority() {
        Selector s1 = new Selector();
        s1.setLabel(Sets.newHashSet("ios", "test"));
        String flowId = "flowA";

        // init:
        AgentPriority p = new AgentPriority();
        p.setSelectorId(s1.getId());

        agentPriorityDao.insert(p);

        // when: add job to priority
        agentPriorityDao.addJob(s1.getId(), flowId, 1L);
        agentPriorityDao.addJob(s1.getId(), flowId, 2L);

        // then: job list should be added
        Optional<AgentPriority> optional = agentPriorityDao.findBySelectorId(s1.getId());
        Assert.assertTrue(optional.isPresent());

        AgentPriority priority = optional.get();
        Assert.assertEquals(2, priority.getQueue().get(flowId).size());

        // when: find min build number for flow
        long minBuildNumber = agentPriorityDao.findMinBuildNumber(s1.getId(), flowId);
        Assert.assertEquals(1L, minBuildNumber);

        // when: remove job from priority
        agentPriorityDao.removeJob(s1.getId(), flowId, 1L);

        // then: build number should be removed
        optional = agentPriorityDao.findBySelectorId(s1.getId());
        Assert.assertTrue(optional.isPresent());

        priority = optional.get();
        Assert.assertEquals(1, priority.getQueue().get(flowId).size());
    }
}
