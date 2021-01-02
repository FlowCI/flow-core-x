package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobPriority;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobPriorityDao extends MongoRepository<JobPriority, String>, CustomJobPriorityDao {

    Optional<JobPriority> findByFlowId(String flowId);

}
