package com.flowci.core.test.task;

import com.flowci.core.task.LocalDockerTask;
import com.flowci.core.task.LocalTaskManager;
import com.flowci.core.task.TaskResult;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.DockerOption;
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
        task.setDocker(new DockerOption().setImage("ubuntu:18.04"));
        task.setScript("echo aaa \n sleep 9999 \n echo bbb");
        task.setTimeoutInSecond(1);

        TaskResult result = localTaskManager.execute(task);
        Assert.assertEquals(0, result.getExitCode());
        Assert.assertNotNull(result.getContainerId());
        Assert.assertNull(result.getErr());
        Assert.assertNotNull(result);
    }
}
