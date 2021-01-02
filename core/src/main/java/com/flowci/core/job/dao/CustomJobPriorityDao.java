package com.flowci.core.job.dao;

public interface CustomJobPriorityDao {

    void addJob(String flowId, String selectorId, Long buildNumber);

    void removeJob(String flowId, String selectorId, Long buildNumber);

    long findMinBuildNumber(String flowId, String selectorId);
}
