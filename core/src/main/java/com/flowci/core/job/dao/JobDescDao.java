package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobDesc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public interface JobDescDao extends MongoRepository<JobDesc, String> {
}
