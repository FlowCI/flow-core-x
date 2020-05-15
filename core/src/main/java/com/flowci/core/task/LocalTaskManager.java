package com.flowci.core.task;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.ScriptBody;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.DockerOption;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class LocalTaskManager {

    @Autowired
    private TaskResultDao taskResultDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private DockerClient dockerClient;

    /**
     * Execute local task, should be run async
     */
    public TaskResult execute(LocalTask task) {
        if (task.hasPlugin()) {
            Plugin plugin = getPlugin(task);

            Optional<String> validate = plugin.verifyInput(task.getInputs());
            if (validate.isPresent()) {
                throw new ArgumentException("The illegal input {0} for plugin {1}", validate.get(), plugin.getName());
            }

            ScriptBody body = (ScriptBody) plugin.getBody();
            task.setScript(body.getScript());
        }

        TaskResult output = new TaskResult();
        output.setName(task.getName());
        output.setJobId(task.getJobId());
        taskResultDao.insert(output);

        if (task instanceof LocalDockerTask) {
            runDockerTask((LocalDockerTask) task, output);
            taskResultDao.save(output);
            return output;
        }

        throw new NotAvailableException("Task is not available");
    }

    private void runDockerTask(LocalDockerTask task, TaskResult r) {
        try {
            DockerOption option = task.getDocker();

            // create
            CreateContainerResponse container = dockerClient.createContainerCmd(option.getImage())
                    .withEntrypoint("/bin/bash")
                    .exec();

            // start
            String containerId = container.getId();
            r.setContainerId(containerId);
            dockerClient.startContainerCmd(container.getId()).exec();

            // print log
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new DockerExecCallBack());

            WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(containerId).exec(resultCallback);
            Integer code = resultCallback.awaitStatusCode(task.getTimeoutInSecond(), TimeUnit.SECONDS);
            r.setExitCode(code);
        } catch (DockerException | DockerClientException e) {
            log.warn(e.getMessage());
            r.setErr(e.getMessage());
        } finally {
            if (r.hasContainerId()) {
                dockerClient.removeContainerCmd(r.getContainerId())
                        .withForce(true)
                        .withRemoveVolumes(true)
                        .exec();
            }
        }
    }

    private Plugin getPlugin(LocalTask task) {
        String name = task.getPlugin();
        GetPluginEvent event = eventManager.publish(new GetPluginEvent(this, name));
        if (Objects.isNull(event.getPlugin())) {
            throw new NotFoundException("The plugin {0} defined in local task not found", name);
        }
        return event.getPlugin();
    }

    private static class DockerExecCallBack implements ResultCallback<Frame> {

        @Override
        public void onStart(Closeable closeable) {
            log.debug("On cmd start");
        }

        @Override
        public void onNext(Frame object) {
            StreamType streamType = object.getStreamType();
            log.debug("[LOCAL-TASK]: {} = {}", streamType, new String(object.getPayload()));
        }

        @Override
        public void onError(Throwable throwable) {
            log.debug("On cmd err");
        }

        @Override
        public void onComplete() {
            log.debug("On cmd complete");
        }

        @Override
        public void close() throws IOException {
            log.debug("On cmd close");
        }
    }
}
