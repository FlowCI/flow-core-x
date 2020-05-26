package com.flowci.core.test.job;

import com.flowci.core.job.dao.ExecutedLocalTaskDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.LocalDockerTask;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.service.LocalTaskServiceImpl;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LocalTaskManagerTest extends SpringScenario {

    @Autowired
    private ExecutedLocalTaskDao executedLocalTaskDao;

    @Autowired
    private LocalTaskServiceImpl localTaskManager;

    @Test
    public void should_execute_local_task() {
        ExecutedLocalTask entity = new ExecutedLocalTask();
        entity.setJobId("test-job-id");
        entity.setName("test");
        executedLocalTaskDao.save(entity);

        LocalDockerTask task = new LocalDockerTask();
        task.setName(entity.getName());
        task.setJobId(entity.getJobId());
        task.setImage("sonarqube:latest");
        task.setScript("echo aaa \n echo bbb");
        task.setTimeoutInSecond(30);

        ExecutedLocalTask result = localTaskManager.execute(task);
        Assert.assertEquals(0, result.getCode().intValue());
        Assert.assertEquals(Executed.Status.SUCCESS, result.getStatus());
        Assert.assertNotNull(result.getContainerId());
        Assert.assertNotNull(result.getStartAt());
        Assert.assertNotNull(result.getFinishAt());
        Assert.assertNull(result.getError());
        Assert.assertNotNull(result);
    }
}
