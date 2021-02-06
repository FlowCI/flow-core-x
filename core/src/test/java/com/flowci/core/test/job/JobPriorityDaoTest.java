package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobPriorityDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobPriority;
import com.flowci.core.test.SpringScenario;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

public class JobPriorityDaoTest extends SpringScenario {

    @Autowired
    private JobPriorityDao jobPriorityDao;

    @Test
    public void should_operate_agent_priority() {
        String flowId = "flowA";

        // init:
        JobPriority p = new JobPriority();
        p.setFlowId(flowId);

        jobPriorityDao.insert(p);

        // when: add job to priority
        jobPriorityDao.addJob(flowId, 1L);
        jobPriorityDao.addJob(flowId, 2L);

        // then: job list should be added
        Optional<JobPriority> optional = jobPriorityDao.findByFlowId(flowId);
        Assert.assertTrue(optional.isPresent());

        JobPriority priority = optional.get();
        Assert.assertEquals(2, priority.getQueue().size());
        Assert.assertEquals(1L, jobPriorityDao.findMinBuildNumber(flowId));

        // when: remove job from priority
        jobPriorityDao.removeJob(flowId, 1L);

        // then: build number should be removed
        optional = jobPriorityDao.findByFlowId(flowId);
        Assert.assertTrue(optional.isPresent());

        priority = optional.get();
        Assert.assertEquals(1, priority.getQueue().size());
        Assert.assertEquals(2L, jobPriorityDao.findMinBuildNumber(flowId));
    }

    @Test
    public void should_get_all_min_buildnumber() {
        // given:
        JobPriority p1 = new JobPriority();
        p1.setFlowId("flowA");
        p1.setQueue(Lists.newArrayList(1L, 2L, 3L));
        jobPriorityDao.save(p1);

        JobPriority p2 = new JobPriority();
        p2.setFlowId("flowB");
        p2.setQueue(Lists.newArrayList(10L, 11L, 12L));
        jobPriorityDao.save(p2);

        // when:
        List<Job> all = jobPriorityDao.findAllMinBuildNumber();
        Assert.assertEquals(2, all.size());

        // then:
        Assert.assertEquals(1L, (long) all.get(0).getBuildNumber());
        Assert.assertEquals(10L, (long) all.get(1).getBuildNumber());
    }
}
