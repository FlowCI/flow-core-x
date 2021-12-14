package com.flowci.core.job.dao;

import com.flowci.core.job.domain.RelatedJobs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RelatedJobsDao extends MongoRepository<RelatedJobs, String>, CustomizedRelatedJobsDao {

    Optional<RelatedJobs> findByGitEventId(String gitEventId);
}
