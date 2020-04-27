package com.flowci.core.job.service;

import com.flowci.core.job.domain.Job;

public interface JobActionService {

    void start(Job job);

    void cancel(Job job);

    Job setJobStatusAndSave(Job job, Job.Status newStatus, String message);
}
