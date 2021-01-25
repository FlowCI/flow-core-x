package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class CustomJobAgentDaoImpl implements CustomJobAgentDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void addFlowToAgent(String jobId, String agentId, String flowPath) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().addToSet(key, flowPath),
                JobAgent.class);
    }

    @Override
    public void removeFlowFromAgent(String jobId, String agentId, String flowPath) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().pull(key, flowPath),
                JobAgent.class);
    }

    @Override
    public void removeAgent(String jobId, String agentId) {
        String key = "agents." + agentId;
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().unset(key),
                JobAgent.class);
    }
}
