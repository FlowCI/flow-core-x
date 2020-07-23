package com.flowci.core.job.manager;

import com.flowci.core.api.adviser.ApiAuth;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.pool.DockerClientExecutor;
import com.flowci.pool.exception.DockerPoolException;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class DockerManager {

    @Autowired
    private String serverUrl;

    @Autowired
    private DockerClientExecutor dockerExecutor;

    public boolean pullImage(String image) throws InterruptedException, DockerPoolException {
        String imageUrl = "docker.io/library/" + image;
        if (image.contains("/")) {
            imageUrl = "docker.io/" + image;
        }


        ListImagesCmd listImagesCmd = client().listImagesCmd().withImageNameFilter(image);
        List<Image> images = dockerExecutor.exec(listImagesCmd);
        if (images.size() >= 1) {
            return true;
        }

        log.info("Image {} not found, will pull", image);
        PullImageCmd pullImageCmd = client().pullImageCmd(imageUrl);
        PullImageCallback callback = dockerExecutor.exec(pullImageCmd, new PullImageCallback());

        boolean await = callback.counter.await(60, TimeUnit.SECONDS);
        if (!await) {
            log.info("Pulling image {} timeout", image);
            return false;
        }

        log.info("Image {} pulled", image);
        return callback.isSuccess;
    }

    public String createAndStartContainer(Option option) throws DockerPoolException {
        StringVars defaultEnv = new StringVars(4);
        defaultEnv.put(Variables.App.Url, serverUrl);
        defaultEnv.put(Variables.Agent.Token, ApiAuth.LocalTaskToken);
        defaultEnv.put(Variables.Agent.Workspace, "/ws/");
        defaultEnv.put(Variables.Agent.PluginDir, "/ws/.plugins");

        CreateContainerCmd createContainerCmd = client().createContainerCmd(option.image)
                .withEnv(defaultEnv.merge(option.inputs).toList())
                .withCmd("/bin/bash", "-c", option.script);

        if (option.hasPlugin()) {
            createContainerCmd.withBinds(
                    new Bind(option.pluginDir, new Volume("/ws/.plugins/" + option.plugin)));
        }

        CreateContainerResponse container = dockerExecutor.exec(createContainerCmd);
        String containerId = container.getId();

        StartContainerCmd startContainerCmd = client().startContainerCmd(container.getId());
        dockerExecutor.exec(startContainerCmd);

        log.debug("Container {} been started", containerId);
        return containerId;
    }

    public void printContainerLog(String cid) throws DockerPoolException {
        LogContainerCmd logContainerCmd = client()
                .logContainerCmd(cid)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true);

        dockerExecutor.exec(logContainerCmd, new FrameCallback());
    }

    /**
     * Wait container to exit or timeout
     *
     * @return true if not running, false for timeout
     */
    public boolean waitContainer(String cid, int timeout) throws DockerPoolException {
        Instant expire = Instant.now().plus(timeout, ChronoUnit.SECONDS);

        for (; ; ) {
            InspectContainerCmd inspectContainerCmd = client().inspectContainerCmd(cid);
            InspectContainerResponse inspect = dockerExecutor.exec(inspectContainerCmd);
            InspectContainerResponse.ContainerState state = inspect.getState();

            if (state.getRunning() == null) {
                return false;
            }

            if (!state.getRunning()) {
                return true;
            }

            if (Instant.now().isAfter(expire)) {
                return false;
            }

            ThreadHelper.sleep(1000);
        }
    }

    public int getContainerExitCode(String cid) throws DockerPoolException {
        InspectContainerCmd inspectContainerCmd = client().inspectContainerCmd(cid);
        InspectContainerResponse inspect = dockerExecutor.exec(inspectContainerCmd);
        return Objects.requireNonNull(inspect.getState().getExitCodeLong()).intValue();
    }

    public void killContainer(String cid) throws DockerPoolException {
        KillContainerCmd killContainerCmd = client().killContainerCmd(cid);
        dockerExecutor.exec(killContainerCmd);
        log.debug("Container {} been killed", cid);
    }

    public void removeContainer(String cid) throws DockerPoolException {
        RemoveContainerCmd removeContainerCmd = client().removeContainerCmd(cid).withForce(true);
        dockerExecutor.exec(removeContainerCmd);
        log.debug("Container {} been removed", cid);
    }

    private DockerClient client() {
        return dockerExecutor.getClient();
    }

    @Getter
    @Setter
    public static final class Option {

        private String image;

        private Vars<String> inputs;

        private String script;

        private String plugin;

        private String pluginDir;

        private boolean hasPlugin() {
            return StringHelper.hasValue(plugin) && StringHelper.hasValue(pluginDir);
        }
    }

    private static abstract class DockerCallback<T> implements ResultCallback<T> {

        protected CountDownLatch counter = new CountDownLatch(1);

        @Override
        public void onStart(Closeable closeable) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {
            counter.countDown();
        }

        @Override
        public void close() throws IOException {

        }
    }

    private static class PullImageCallback extends DockerCallback<PullResponseItem> {

        private boolean isSuccess = false;

        @Override
        public void onNext(PullResponseItem object) {
            isSuccess = object.isPullSuccessIndicated();
        }
    }

    private static class FrameCallback extends DockerCallback<Frame> {

        @Override
        public void onNext(Frame object) {
            StreamType streamType = object.getStreamType();
            log.debug("[LOCAL-TASK]: {} = {}", streamType, new String(object.getPayload()));
        }
    }
}
