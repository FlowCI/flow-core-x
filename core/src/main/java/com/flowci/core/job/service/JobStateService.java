package com.flowci.core.job.service;

import com.flowci.core.job.domain.Job;

public interface JobStateService {

    void onCancel();

    void update(Job job, Job.Status target);

    Job setJobStatusAndSave(Job job, Job.Status newStatus, String message);
}
