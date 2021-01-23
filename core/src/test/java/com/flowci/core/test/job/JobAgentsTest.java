package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobAgents;
import com.flowci.core.test.SpringScenario;
import com.flowci.tree.FlowNode;
import com.flowci.tree.ParallelStepNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

import java.util.List;
import java.util.Optional;

public class JobAgentsTest extends SpringScenario {

    @Autowired
    private JobDao jobDao;

    @Test
    public void should_list_reuse_candidates() {
        FlowNode flow = new FlowNode("flow");
        ParallelStepNode parallel = new ParallelStepNode("parallel", flow);
        FlowNode subflow1 = new FlowNode("sub_1", parallel);
        FlowNode subflow2 = new FlowNode("sub_2", parallel);

        JobAgents ja = new JobAgents();

        // find a1 for root flow
        ja.save("a1", flow);

        // try to get agent for subflow1
        Optional<String> hasAgent = ja.getAgent(subflow1);
        Assert.assertFalse(hasAgent.isPresent());

        // get candidate agents for subflow1
        List<String> candidates = ja.getCandidates(subflow1);
        Assert.assertEquals(1, candidates.size());
        Assert.assertEquals("a1", candidates.get(0));

        // found a1 is match subflow1
        ja.save("a1", subflow1);
        Assert.assertTrue(ja.getAgent(subflow1).isPresent());

        // try to get agent for subflow2
        Assert.assertFalse(ja.getAgent(subflow2).isPresent());

        // shouldn't get candidate for subflow2 since a1 is occupied
        Assert.assertEquals(0, ja.getCandidates(subflow2).size());

        // when subflow1 finished, remove path
        ja.remove("a1", subflow1);

        // should get candidate for subflow2
        Assert.assertEquals(1, ja.getCandidates(subflow2).size());
    }

    @Test
    public void should_save_job_agents() {
        FlowNode flowA = new FlowNode("flowA");
        FlowNode flowB = new FlowNode("flowB");

        Job job = new Job();
        job.getAgents().save("111", flowA);
        job.getAgents().save("111", flowB);

        job = jobDao.save(job);
        Assert.assertEquals(1, job.getAgents().all().size());

        job.getAgents().remove("111", flowA);
        job.getAgents().remove("111", flowB);
        job = jobDao.save(job);
        Assert.assertEquals(1, job.getAgents().all().size());
    }
}
