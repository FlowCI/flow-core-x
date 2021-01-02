package com.flowci.core.agent.dao;

public interface CustomAgentPriorityDao {

    void addJob(String selectorId, String flowId, Long buildNumber);

    void removeJob(String selectorId, String flowId, Long buildNumber);

    long findMinBuildNumber(String selectorId, String flowId);
}
