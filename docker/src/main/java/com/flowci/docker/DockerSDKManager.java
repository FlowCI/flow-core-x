package com.flowci.docker;

import com.flowci.docker.domain.DockerCallback;
import com.flowci.docker.domain.DockerStartOption;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Log4j2
public class DockerSDKManager implements DockerManager {

    private final String dockerHost;

    private final ContainerManager containerManager = new ContainerManagerImpl();

    private final ImageManager imageManager = new ImageMangerImpl();

    public DockerSDKManager(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    @Override
    public ContainerManager getContainerManager() {
        return containerManager;
    }

    @Override
    public ImageManager getImageManager() {
        return imageManager;
    }

    private class ImageMangerImpl implements ImageManager {

        @Override
        public void pull(String image, int timeoutInSeconds, Consumer<PullResponseItem> progress) throws Exception {
            String imageUrl = "docker.io/library/" + image;
            if (image.contains("/")) {
                imageUrl = "docker.io/" + image;
            }

            try (DockerClient client = newClient()) {
                if (findImage(client, image).size() >= 1) {
                    return;
                }

                PullImageCallback callback = client.pullImageCmd(imageUrl).exec(new PullImageCallback(progress));
                boolean await = callback.getCounter().await(timeoutInSeconds, TimeUnit.SECONDS);

                if (!await) {
                    throw new DockerException(String.format("Timeout when pull image %s", image), 0);
                }

                if (findImage(client, image).isEmpty()) {
                    throw new DockerException(String.format("Failed on pull image %s", image), 0);
                }
            }
        }

        private List<Image> findImage(DockerClient client, String image) {
            return client.listImagesCmd().withImageNameFilter(image).exec();
        }
    }

    private class ContainerManagerImpl implements ContainerManager {

        @Override
        public List<Container> list(String statusFilter, String nameFilter) throws Exception {
            try (DockerClient client = newClient()) {
                ListContainersCmd cmd = client.listContainersCmd().withShowAll(true);

                if (StringHelper.hasValue(nameFilter)) {
                    cmd.withNameFilter(Lists.newArrayList(nameFilter));
                }

                if (StringHelper.hasValue(statusFilter)) {
                    cmd.withStatusFilter(Lists.newArrayList(statusFilter));
                }

                return cmd.exec();
            }
        }

        @Override
        public InspectContainerResponse inspect(String containerId) throws Exception {
            try (DockerClient client = newClient()) {
                return client.inspectContainerCmd(containerId).exec();
            }
        }

        @Override
        public String start(DockerStartOption option) throws Exception {
            try (DockerClient client = newClient()) {
                CreateContainerCmd createCmd = client.createContainerCmd(option.getImage());
                createCmd.withEnv(option.toEnvList());
                createCmd.withBinds(option.toBindList());
                createCmd.withEntrypoint(option.getEntrypoint());

                if (option.hasName()) {
                    createCmd.withName(option.getName());
                }

                CreateContainerResponse container = createCmd.exec();
                client.startContainerCmd(container.getId()).exec();
                return container.getId();
            }
        }

        @Override
        public void wait(String containerId, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {
            Instant expire = Instant.now().plus(timeoutInSeconds, ChronoUnit.SECONDS);

            try (DockerClient client = newClient()) {
                if (onLog != null) {
                    client.logContainerCmd(containerId)
                            .withStdOut(true)
                            .withStdErr(true)
                            .withFollowStream(true)
                            .exec(new FrameCallback(onLog));
                }

                for (; ; ) {
                    InspectContainerResponse.ContainerState state = client.inspectContainerCmd(containerId).exec().getState();
                    if (state.getRunning() == null || !state.getRunning()) {
                        return;
                    }

                    if (Instant.now().isAfter(expire)) {
                        throw new DockerException("timeout", 0);
                    }

                    Thread.sleep(2000);
                }
            }
        }

        @Override
        public void stop(String containerId) throws Exception {
            try (DockerClient client = newClient()) {
                InspectContainerResponse.ContainerState state = client.inspectContainerCmd(containerId).exec().getState();
                Boolean running = state.getRunning();
                if (running != null && running) {
                    client.stopContainerCmd(containerId).exec();
                }
            }
        }

        @Override
        public void resume(String containerId) throws Exception {
            try (DockerClient client = newClient()) {
                client.removeContainerCmd(containerId).exec();
            }
        }

        @Override
        public void delete(String containerId) throws Exception {
            try (DockerClient client = newClient()) {
                RemoveContainerCmd removeCmd = client.removeContainerCmd(containerId).withForce(true);
                removeCmd.exec();
            }
        }
    }

    private DockerClient newClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost).build();
        return DockerClientBuilder.getInstance(config).build();
    }

    private static class PullImageCallback extends DockerCallback<PullResponseItem> {

        private final Consumer<PullResponseItem> progress;

        private PullImageCallback(Consumer<PullResponseItem> progress) {
            this.progress = progress;
        }

        @Override
        public void onNext(PullResponseItem item) {
            if (progress != null && item != null) {
                progress.accept(item);
            }
        }
    }

    private static class FrameCallback extends DockerCallback<Frame> {

        private final Consumer<Frame> onLog;

        private FrameCallback(Consumer<Frame> onLog) {
            this.onLog = onLog;
        }

        @Override
        public void onNext(Frame object) {
            if (this.onLog != null) {
                onLog.accept(object);
            }
        }
    }
}
