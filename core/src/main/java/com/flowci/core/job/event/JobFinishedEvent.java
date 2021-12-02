package com.flowci.core.job.event;

import com.flowci.core.job.domain.Job;

public class JobFinishedEvent extends JobEvent {

    public JobFinishedEvent(Object source, Job job) {
        super(source, job);
    }
}
