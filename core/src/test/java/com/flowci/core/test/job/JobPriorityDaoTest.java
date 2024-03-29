package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobPriorityDao;
import com.flowci.core.job.domain.JobKey;
import com.flowci.core.job.domain.JobPriority;
import com.flowci.core.test.SpringScenario;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JobPriorityDaoTest extends SpringScenario {

    @Autowired
    private JobPriorityDao jobPriorityDao;

    @Test
    void should_operate_agent_priority() {
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
        assertTrue(optional.isPresent());

        JobPriority priority = optional.get();
        assertEquals(2, priority.getQueue().size());
        assertEquals(1L, jobPriorityDao.findMinBuildNumber(flowId));

        // when: remove job from priority
        jobPriorityDao.removeJob(flowId, 1L);

        // then: build number should be removed
        optional = jobPriorityDao.findByFlowId(flowId);
        assertTrue(optional.isPresent());

        priority = optional.get();
        assertEquals(1, priority.getQueue().size());
        assertEquals(2L, jobPriorityDao.findMinBuildNumber(flowId));
    }

    @Test
    void should_get_all_min_buildnumber() {
        // given:
        JobPriority p1 = new JobPriority();
        p1.setFlowId("flowA");
        p1.setQueue(Lists.newArrayList(1L, 2L, 3L));
        jobPriorityDao.save(p1);

        JobPriority p2 = new JobPriority();
        p2.setFlowId("flowB");
        p2.setQueue(Lists.newArrayList(10L, 11L, 12L));
        jobPriorityDao.save(p2);

        JobPriority p3 = new JobPriority();
        p3.setFlowId("flowC");
        p3.setQueue(Lists.newArrayList());
        jobPriorityDao.save(p3);

        // when:
        List<JobKey> all = jobPriorityDao.findAllMinBuildNumber();
        assertEquals(3, all.size());

        // then:
        assertEquals(1L, (long) all.get(0).getBuildNumber());
        assertEquals(10L, (long) all.get(1).getBuildNumber());
        assertNull(all.get(2).getBuildNumber());
    }
}
