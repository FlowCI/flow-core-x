package com.flowci.docker;

import com.flowci.docker.domain.DockerStartOption;
import com.flowci.util.ObjectsHelper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.List;

public class DockerSDKManager implements DockerManager {

    private final String dockerHost;

    private final ContainerManager containerManager = new ContainerManagerImpl();

    public DockerSDKManager(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    @Override
    public ContainerManager getContainerManager() {
        return containerManager;
    }

    private class ContainerManagerImpl implements ContainerManager {

        @Override
        public List<Container> list(List<String> statusFilter, List<String> nameFilter) throws Exception {
            try (DockerClient client = newClient()) {
                ListContainersCmd cmd = client.listContainersCmd().withShowAll(true);

                if (ObjectsHelper.hasCollection(nameFilter)) {
                    cmd.withNameFilter(nameFilter);
                }

                if (ObjectsHelper.hasCollection(statusFilter)) {
                    cmd.withStatusFilter(statusFilter);
                }

                return cmd.exec();
            }
        }

        @Override
        public String start(DockerStartOption option) throws Exception {
            try (DockerClient client = newClient()) {
                CreateContainerCmd createCmd = client.createContainerCmd(option.getImage());
                createCmd.withEnv(option.toEnvList());
                createCmd.withBinds(option.toBindList());

                if (option.hasName()) {
                    createCmd.withName(option.getName());
                }

                CreateContainerResponse container = createCmd.exec();
                client.startContainerCmd(container.getId()).exec();
                return container.getId();
            }
        }

        @Override
        public void stop(String containerId) throws Exception {
            try (DockerClient client = newClient()) {
                InspectContainerResponse exec = client.inspectContainerCmd(containerId).exec();
                InspectContainerResponse.ContainerState state = exec.getState();
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
}
