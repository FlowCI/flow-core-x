package com.flowci.core.job.service;

import com.flowci.core.job.domain.Step;
import com.flowci.core.job.domain.Job;
import com.flowci.exception.CIException;

public interface JobActionService {

    void toLoading(Job job);

    void toCreated(Job job, String yml);

    void toStart(Job job);

    void toRun(Job job);

    void toContinue(Job job, Step step);

    void toCancelled(Job job, CIException exception);

    void toTimeout(Job job);
}
