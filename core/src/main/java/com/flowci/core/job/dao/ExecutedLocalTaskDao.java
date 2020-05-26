package com.flowci.core.job.dao;

import com.flowci.core.job.domain.ExecutedLocalTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutedLocalTaskDao extends MongoRepository<ExecutedLocalTask, String> {

    List<ExecutedLocalTask> findAllByJobId(String jobId);
}
