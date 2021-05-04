package com.flowci.core.job.service;

import com.flowci.core.job.domain.Step;
import com.flowci.exception.CIException;

public interface JobActionService {

    void toLoading(String jobId);

    void toCreated(String jobId, String yml);

    void toStart(String jobId);

    void toRun(String jobId);

    void toContinue(Step step);

    void toCancelled(String jobId, CIException exception);

    void toTimeout(String jobId);
}
