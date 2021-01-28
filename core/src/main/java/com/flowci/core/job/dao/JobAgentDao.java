package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobAgent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobAgentDao extends MongoRepository<JobAgent, String>, CustomJobAgentDao {

    void deleteAllByFlowId(String flowId);
}
