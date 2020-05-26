package com.flowci.core.job.service;

import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.domain.LocalTask;

import java.util.List;

public interface LocalTaskService {

    void init(Job job);

    List<ExecutedLocalTask> list(Job job);

    void executeAsync(Job job, LocalTask task);

    ExecutedLocalTask execute(Job job, LocalTask task);
}
