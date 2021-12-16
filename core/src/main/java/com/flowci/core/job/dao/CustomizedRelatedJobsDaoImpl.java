package com.flowci.core.job.dao;

import com.flowci.core.job.domain.RelatedJobs;
import com.flowci.core.job.domain.JobDesc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomizedRelatedJobsDaoImpl implements CustomizedRelatedJobsDao {

    private final static String FieldGitEventId = "gitEventId";

    private final static String FieldJobs = "jobs";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void addRelatedInfo(String gitEventId, JobDesc job) {
        mongoTemplate.upsert(
                new Query().addCriteria(Criteria.where(FieldGitEventId).is(gitEventId)),
                new Update().push(FieldJobs, job),
                RelatedJobs.class);
    }
}
