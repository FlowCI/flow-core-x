package com.flowci.core.job.dao;

import com.flowci.core.job.domain.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class CustomJobDaoImpl implements CustomJobDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void increaseNumOfArtifact(String jobId) {
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().inc("numOfArtifact", 1),
                Job.class);
    }
}
