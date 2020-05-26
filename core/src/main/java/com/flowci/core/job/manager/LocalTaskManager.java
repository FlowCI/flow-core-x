package com.flowci.core.job.manager;

import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.LocalDockerTask;

public interface LocalTaskManager {

    void executeAsync(LocalDockerTask task);

    ExecutedLocalTask execute(LocalDockerTask task);
}
