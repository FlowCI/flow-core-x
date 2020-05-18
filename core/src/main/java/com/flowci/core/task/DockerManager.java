package com.flowci.core.task;

import com.flowci.core.common.helper.ThreadHelper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
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
    private DockerClient client;

    public boolean pullImage(String image) throws InterruptedException {
        String imageUrl = "docker.io/library/" + image;
        if (image.contains("/")) {
            imageUrl = "docker.io/" + image;
        }

        List<Image> images = client.listImagesCmd().withImageNameFilter(image).exec();
        if (images.size() >= 1) {
            return true;
        }

        log.info("Image {} not found, will pull", image);
        PullImageCallback callback = client.pullImageCmd(imageUrl).exec(new PullImageCallback());

        boolean await = callback.counter.await(60, TimeUnit.SECONDS);
        if (!await) {
            log.info("Pulling image {} timeout", image);
            return false;
        }

        log.info("Image {} pulled", image);
        return callback.isSuccess;
    }

    public String createAndStartContainer(String image, List<String> envs, String bash) {
        CreateContainerResponse container = client.createContainerCmd(image)
                .withEnv(envs)
                .withCmd("/bin/bash", "-c", bash)
                .exec();

        String containerId = container.getId();
        client.startContainerCmd(container.getId()).exec();
        return containerId;
    }

    public void printContainerLog(String cid) {
        client.logContainerCmd(cid)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new FrameCallback());
    }

    /**
     * Wait container to exit or timeout
     *
     * @return true if not running, false for timeout
     */
    public boolean waitContainer(String cid, int timeout) {
        Instant expire = Instant.now().plus(timeout, ChronoUnit.SECONDS);

        for (; ; ) {
            InspectContainerResponse inspect = client.inspectContainerCmd(cid).exec();
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

    public int getContainerExitCode(String cid) {
        InspectContainerResponse inspect = client.inspectContainerCmd(cid).exec();
        return Objects.requireNonNull(inspect.getState().getExitCodeLong()).intValue();
    }

    public void killContainer(String cid) {
        client.killContainerCmd(cid).exec();
    }

    public void removeContainer(String cid) {
        client.removeContainerCmd(cid).withForce(true).exec();
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
