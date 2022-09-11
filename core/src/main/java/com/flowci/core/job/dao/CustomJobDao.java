package com.flowci.core.job.dao;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobNumber;

import java.util.Collection;
import java.util.List;

public interface CustomJobDao {

    void increaseNumOfArtifact(String jobId);

    List<Job> list(Collection<JobNumber> numbers);
}
