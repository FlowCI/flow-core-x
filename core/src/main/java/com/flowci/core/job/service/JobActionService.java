package com.flowci.core.job.service;

import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;

public interface JobActionService {

    void toLoading(Job job);

    void toCreated(Job job, String yml);

    void toStart(Job job);

    void toRun(Job job);

    void toContinue(Job job, ExecutedCmd step);

    void toCancelled(Job job, String reason);

    void toTimeout(Job job);
}
