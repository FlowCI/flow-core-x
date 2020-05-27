package com.flowci.core.job.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.domain.LocalTask;

import java.util.List;

public interface LocalTaskService {

    void init(Job job);

    List<ExecutedLocalTask> list(Job job);

    Long delete(Job job);

    Long delete(Flow flow);

    void executeAsync(Job job);

    ExecutedLocalTask execute(Job job, LocalTask task);
}
