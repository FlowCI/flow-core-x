package com.flowci.core.test.job;

import com.flowci.core.job.domain.LocalDockerTask;
import com.flowci.core.job.domain.TaskResult;
import com.flowci.core.job.manager.LocalTaskManager;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LocalTaskManagerTest extends SpringScenario {

    @Autowired
    private LocalTaskManager localTaskManager;

    @Test
    public void should_execute_local_task() {
        LocalDockerTask task = new LocalDockerTask();
        task.setName("test");
        task.setJobId("test-job-id");
        task.setImage("sonarqube:latest");
        task.setScript("echo aaa \n echo bbb");
        task.setTimeoutInSecond(30);

        TaskResult result = localTaskManager.execute(task);
        Assert.assertEquals(0, result.getExitCode());
        Assert.assertNotNull(result.getContainerId());
        Assert.assertNull(result.getErr());
        Assert.assertNotNull(result);
    }
}
