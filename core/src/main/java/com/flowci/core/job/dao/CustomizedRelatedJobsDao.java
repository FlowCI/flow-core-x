package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobDesc;

public interface CustomizedRelatedJobsDao {

    void addRelatedInfo(String gitEventId, JobDesc desc);
}
