package com.flowci.core.job.dao;

import com.flowci.core.job.domain.Job;

import java.util.List;

public interface CustomJobPriorityDao {

    void addJob(String flowId, Long buildNumber);

    void removeJob(String flowId, Long buildNumber);

    long findMinBuildNumber(String flowId);

    // only return job with flow id and build number
    List<Job> findAllMinBuildNumber();
}
