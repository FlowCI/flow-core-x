package com.flowci.core.job.dao;

public interface CustomJobPriorityDao {

    void addJob(String flowId, Long buildNumber);

    void removeJob(String flowId, Long buildNumber);

    long findMinBuildNumber(String flowId);
}
