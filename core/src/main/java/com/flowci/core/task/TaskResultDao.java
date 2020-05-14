package com.flowci.core.task;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskResultDao extends MongoRepository<TaskResult, String> {
}
