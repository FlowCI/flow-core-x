package com.flowci.build.repo;

import com.flowci.build.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JobRepo extends JpaRepository<Job, Job.Id> {

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Job j set j.status = ?2 where j.id.buildId= ?1")
    void updateJobStatusByBuildId(Long buildId, Job.Status status);
}
