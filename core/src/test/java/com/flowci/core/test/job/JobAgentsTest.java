package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobAgentDao;
import com.flowci.core.job.domain.JobAgent;
import com.flowci.core.test.SpringScenario;
import com.flowci.tree.FlowNode;
import com.flowci.tree.ParallelStepNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JobAgentsTest extends SpringScenario {

    @Autowired
    private JobAgentDao jobAgentDao;

    @Test
    void should_add_remove_agents() {
        String jobId = "1234556";
        jobAgentDao.save(new JobAgent(jobId, "1"));

        jobAgentDao.addFlowToAgent(jobId, "agent1", "/root");
        JobAgent jobAgent = jobAgentDao.findById(jobId).get();
        assertTrue(jobAgent.getAgents().containsKey("agent1"));

        jobAgentDao.removeFlowFromAgent(jobId, "agent1", "/root");
        assertTrue(jobAgentDao.findById(jobId).get().getAgents().containsKey("agent1"));
    }

    @Test
    void should_list_reuse_candidates() {
        FlowNode flow = new FlowNode("flow");
        ParallelStepNode parallel = new ParallelStepNode("parallel", flow);
        FlowNode subflow1 = new FlowNode("sub_1", parallel);
        FlowNode subflow2 = new FlowNode("sub_2", parallel);

        JobAgent ja = new JobAgent();

        // find a1 for root flow
        ja.save("a1", flow);

        // try to get agent for subflow1
        Optional<String> hasAgent = ja.getAgent(subflow1);
        assertFalse(hasAgent.isPresent());

        // get candidate agents for subflow1
        List<String> candidates = ja.getCandidates(subflow1);
        assertEquals(1, candidates.size());
        assertEquals("a1", candidates.get(0));

        // found a1 is match subflow1
        ja.save("a1", subflow1);
        assertTrue(ja.getAgent(subflow1).isPresent());

        // try to get agent for subflow2
        assertFalse(ja.getAgent(subflow2).isPresent());

        // shouldn't get candidate for subflow2 since a1 is occupied
        assertEquals(0, ja.getCandidates(subflow2).size());

        // when subflow1 finished, remove path
        ja.remove("a1", subflow1);

        // should get candidate for subflow2
        assertEquals(1, ja.getCandidates(subflow2).size());
    }
}
