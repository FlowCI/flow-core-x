package com.flowci.core.job.service;

import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;

public interface JobActionService {

    void toStart(Job job);

    void toRun(Job job);

    void toContinue(Job job, ExecutedCmd step);

    void toCancel(Job job);

    Job setJobStatusAndSave(Job job, Job.Status newStatus, String message);
}
