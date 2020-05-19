package com.flowci.core.task.dao;

import com.flowci.core.task.domain.TaskResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskResultDao extends MongoRepository<TaskResult, String> {
}
