package com.flowci.core.task.manager;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.ScriptBody;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.task.dao.TaskResultDao;
import com.flowci.core.task.domain.LocalDockerTask;
import com.flowci.core.task.domain.LocalTask;
import com.flowci.core.task.domain.TaskResult;
import com.flowci.core.task.event.StartAsyncLocalTaskEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.ObjectsHelper;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Log4j2
@Component
public class LocalTaskManager {

    @Autowired
    private TaskResultDao taskResultDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private DockerManager dockerManager;

    @Autowired
    private ThreadPoolTaskExecutor localTaskExecutor;

    @EventListener
    public void onStartTaskEvent(StartAsyncLocalTaskEvent event) {
        localTaskExecutor.execute(() -> {
            LocalTask task = event.getTask();
            if (task instanceof LocalDockerTask) {
                execute((LocalDockerTask) task);
            }
        });
    }

    /**
     * Execute local task, should be run async
     */
    public TaskResult execute(LocalDockerTask task) {
        log.info("Start local task {} image = {} for job {}", task.getName(), task.getImage(), task.getJobId());

        if (task.hasPlugin()) {
            String name = task.getPlugin();
            GetPluginEvent event = eventManager.publish(new GetPluginEvent(this, name));

            if (Objects.isNull(event.getPlugin())) {
                throw new NotFoundException("The plugin {0} defined in local task not found", name);
            }

            Plugin plugin = event.getPlugin();
            Optional<String> validate = plugin.verifyInput(task.getInputs());
            if (validate.isPresent()) {
                throw new ArgumentException("The illegal input {0} for plugin {1}", validate.get(), plugin.getName());
            }

            ScriptBody body = (ScriptBody) plugin.getBody();
            task.setScript(body.getScript());
            task.setPluginDir(event.getDir());

            // apply docker image only from plugin if it's specified
            ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
                task.setImage(plugin.getDocker().getImage());
            });
        }

        TaskResult output = new TaskResult();
        output.setName(task.getName());
        output.setJobId(task.getJobId());
        taskResultDao.insert(output);

        runDockerTask(task, output);
        taskResultDao.save(output);
        return output;
    }

    private void runDockerTask(LocalDockerTask task, TaskResult r) {
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

            r.setExitCode(dockerManager.getContainerExitCode(cid));

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
