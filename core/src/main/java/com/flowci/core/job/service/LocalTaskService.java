package com.flowci.core.job.service;

import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.LocalDockerTask;

import java.util.List;

public interface LocalTaskService {

    void init(Job job);

    List<ExecutedLocalTask> list(Job job);

    void executeAsync(LocalDockerTask task);

    ExecutedLocalTask execute(LocalDockerTask task);
}
