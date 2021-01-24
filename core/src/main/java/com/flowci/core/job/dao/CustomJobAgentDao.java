package com.flowci.core.job.dao;

public interface CustomJobAgentDao {

    void addFlowToAgent(String jobId, String agentId, String flowPath);

    void removeFlowFromAgent(String jobId, String agentId, String flowPath);
}
