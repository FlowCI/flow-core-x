package com.flowci.core.job.service;

import com.flowci.core.agent.domain.ShellOut;
import com.flowci.exception.CIException;

public interface JobActionService {

    void toLoading(String jobId);

    void toCreated(String jobId, String yml);

    void toStart(String jobId);

    void toRun(String jobId);

    void toContinue(String jobId, ShellOut shellOut);

    void toCancelled(String jobId, CIException exception);

    void toTimeout(String jobId);
}
