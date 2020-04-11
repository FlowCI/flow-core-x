package com.flowci.pool.manager;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.DockerPoolException;
import com.flowci.util.UnixHelper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.collect.Lists;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.flowci.pool.domain.AgentContainer.NameFilter;
import static com.flowci.pool.domain.AgentContainer.name;
import static com.flowci.pool.domain.StartContext.AgentEnvs.*;

public class SocketPoolManager implements PoolManager<SocketInitContext> {

    private DockerClient client;

    /**
     * Init local docker.sock api interface
     */
    @Override
    public void init(SocketInitContext context) throws Exception {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(context.getDockerHost()).build();

        client = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public List<AgentContainer> list(Optional<String> state) throws DockerPoolException {
        ListContainersCmd cmd = client.listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(NameFilter));

        state.ifPresent((s) -> {
            cmd.withStatusFilter(Lists.newArrayList(state.get()));
        });

        try {

            List<Container> list = cmd.exec();
            List<AgentContainer> result = new ArrayList<>(list.size());
            for (Container item : list) {
                String name = item.getNames()[0];
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                result.add(AgentContainer.of(item.getId(), name, item.getState()));
            }
            return result;
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public int size() throws DockerPoolException {
        try {
            return client.listContainersCmd()
                    .withShowAll(true)
                    .withNameFilter(Lists.newArrayList(NameFilter))
                    .exec()
                    .size();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void start(StartContext context) throws DockerPoolException {
        try {
            final String name = name(context.getAgentName());
            final Path srcDirOnHost = UnixHelper.replacePathWithEnv(context.getDirOnHost());

            CreateContainerResponse container = client.createContainerCmd(AgentContainer.Image).withName(name)
                    .withEnv(String.format("%s=%s", SERVER_URL, context.getServerUrl()),
                            String.format("%s=%s", AGENT_TOKEN, context.getToken()),
                            String.format("%s=%s", AGENT_LOG_LEVEL, context.getLogLevel()),
                            String.format("%s=%s", AGENT_WORKSPACE, "/ws"),
                            String.format("%s=%s", AGENT_VOLUMES, System.getenv(AGENT_VOLUMES)))
                    .withBinds(
                            new Bind(srcDirOnHost.toString(), new Volume("/ws")),
                            new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
                    .exec();

            client.startContainerCmd(container.getId()).exec();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void stop(String name) throws DockerPoolException {
        try {
            client.stopContainerCmd(findContainer(name).getId()).exec();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void resume(String name) throws DockerPoolException {
        try {
            Container c = findContainer(name);
            client.startContainerCmd(c.getId()).exec();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void remove(String name) throws DockerPoolException {
        try {
            client.removeContainerCmd(findContainer(name).getId()).withForce(true).exec();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public String status(String name) {
        try {
            return findContainer(name).getState();
        } catch (DockerPoolException e) {
            return DockerStatus.None;
        }
    }

    private Container findContainer(String name) throws DockerPoolException {
        String containerName = name(name);
        List<Container> list = client.listContainersCmd().withShowAll(true).withNameFilter(Lists.newArrayList(containerName))
                .exec();

        if (list.size() != 1) {
            throw new DockerPoolException("Unable to find container for agent {0}", containerName);
        }

        return list.get(0);
    }
}