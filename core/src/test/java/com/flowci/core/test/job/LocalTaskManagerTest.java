package com.flowci.core.test.job;

import com.flowci.core.job.dao.ExecutedLocalTaskDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.service.LocalTaskServiceImpl;
import com.flowci.core.plugin.dao.PluginDao;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.DockerOption;
import com.flowci.domain.LocalTask;
import com.flowci.domain.StringVars;
import com.flowci.domain.Version;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LocalTaskManagerTest extends SpringScenario {

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private ExecutedLocalTaskDao executedLocalTaskDao;

    @Autowired
    private LocalTaskServiceImpl localTaskManager;

    @Test
    public void should_execute_local_task() {
        // init:
        Job job = new Job();
        job.setId("test-job-id");
        job.setContext(new StringVars().putAndReturn("T1", "hello"));

        Plugin p = new Plugin();
        p.setName("test-plugin");
        p.setVersion(Version.parse("0.1.0"));
        p.setBash("echo aaa \n echo bbb");
        p.setDocker(new DockerOption().setImage("sonarqube:latest"));
        pluginDao.save(p);

        ExecutedLocalTask entity = new ExecutedLocalTask();
        entity.setJobId(job.getId());
        entity.setName(p.getName());
        executedLocalTaskDao.save(entity);

        LocalTask task = new LocalTask();
        task.setPlugin(entity.getName());
        task.setEnvs(new StringVars().putAndReturn("T1", "world"));

        ExecutedLocalTask result = localTaskManager.execute(job, task);
        Assert.assertEquals(0, result.getCode().intValue());
        Assert.assertEquals(Executed.Status.SUCCESS, result.getStatus());
        Assert.assertNotNull(result.getContainerId());
        Assert.assertNotNull(result.getStartAt());
        Assert.assertNotNull(result.getFinishAt());
        Assert.assertNull(result.getError());
        Assert.assertNotNull(result);
    }
}
