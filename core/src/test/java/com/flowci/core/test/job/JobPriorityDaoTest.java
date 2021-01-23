package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobPriorityDao;
import com.flowci.core.job.domain.JobPriority;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class JobPriorityDaoTest extends SpringScenario {

    @Autowired
    private JobPriorityDao agentPriorityDao;

    @Test
    public void should_operate_agent_priority() {
        String flowId = "flowA";

        // init:
        JobPriority p = new JobPriority();
        p.setFlowId(flowId);

        agentPriorityDao.insert(p);

        // when: add job to priority
        agentPriorityDao.addJob(flowId, 1L);
        agentPriorityDao.addJob(flowId, 2L);

        // then: job list should be added
        Optional<JobPriority> optional = agentPriorityDao.findByFlowId(flowId);
        Assert.assertTrue(optional.isPresent());

        JobPriority priority = optional.get();
        Assert.assertEquals(2, priority.getQueue().size());
        Assert.assertEquals(1L, agentPriorityDao.findMinBuildNumber(flowId));

        // when: remove job from priority
        agentPriorityDao.removeJob(flowId, 1L);

        // then: build number should be removed
        optional = agentPriorityDao.findByFlowId(flowId);
        Assert.assertTrue(optional.isPresent());

        priority = optional.get();
        Assert.assertEquals(1, priority.getQueue().size());
        Assert.assertEquals(2L, agentPriorityDao.findMinBuildNumber(flowId));
    }
}
