package com.flowci.core.job.dao;

import com.flowci.core.job.domain.TaskResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskResultDao extends MongoRepository<TaskResult, String> {
}
