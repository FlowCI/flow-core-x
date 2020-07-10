package com.flowci.core.job.manager;

import com.flowci.core.job.domain.Step;
import com.flowci.core.job.domain.Job;

public interface JobActionManager {

    void toLoading(Job job);

    void toCreated(Job job, String yml);

    void toStart(Job job);

    void toRun(Job job);

    void toContinue(Job job, Step step);

    void toCancelled(Job job, String reason);

    void toTimeout(Job job);
}
