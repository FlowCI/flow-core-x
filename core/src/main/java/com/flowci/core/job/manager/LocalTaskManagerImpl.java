package com.flowci.core.job.manager;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.dao.ExecutedLocalTaskDao;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.LocalDockerTask;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.exception.NotAvailableException;
import com.flowci.util.ObjectsHelper;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class LocalTaskManagerImpl implements LocalTaskManager {

    @Autowired
    private ExecutedLocalTaskDao executedLocalTaskDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private DockerManager dockerManager;

    @Autowired
    private ThreadPoolTaskExecutor localTaskExecutor;

    @Override
    public void executeAsync(LocalDockerTask task) {
        localTaskExecutor.execute(() -> execute(task));
    }

    @Override
    public ExecutedLocalTask execute(LocalDockerTask task) {
        ExecutedLocalTask output = new ExecutedLocalTask();
        output.setName(task.getName());
        output.setJobId(task.getJobId());
        executedLocalTaskDao.insert(output);

        if (task.hasPlugin()) {
            String name = task.getPlugin();
            GetPluginEvent event = eventManager.publish(new GetPluginAndVerifySetContext(this, name, task.getInputs()));

            if (event.hasError()) {
                output.setErr(event.getError().getMessage());
                executedLocalTaskDao.save(output);
                return output;
            }

            Plugin plugin = event.getFetched();
            task.setScript(plugin.getScript());
            task.setPluginDir(event.getDir());

            // apply docker image only from plugin if it's specified
            ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
                task.setImage(plugin.getDocker().getImage());
            });
        }

        log.info("Start local task {} image = {} for job {}", task.getName(), task.getImage(), task.getJobId());
        runDockerTask(task, output);
        executedLocalTaskDao.save(output);
        return output;
    }

    private void runDockerTask(LocalDockerTask task, ExecutedLocalTask r) {
        try {
            String image = task.getImage();

            boolean isSuccess = dockerManager.pullImage(image);
            if (!isSuccess) {
                throw new NotAvailableException("Docker image {0} not available", image);
            }

            String cid = dockerManager.createAndStartContainer(task);
            r.setContainerId(cid);
            dockerManager.printContainerLog(cid);

            if (!dockerManager.waitContainer(cid, task.getTimeoutInSecond())) {
                dockerManager.killContainer(cid);
            }

            r.setCode(dockerManager.getContainerExitCode(cid));
        } catch (DockerException | DockerClientException | InterruptedException e) {
            log.warn(e.getMessage());
            r.setErr(e.getMessage());
        } finally {
            if (r.hasContainerId()) {
                dockerManager.removeContainer(r.getContainerId());
            }
        }
    }
}
