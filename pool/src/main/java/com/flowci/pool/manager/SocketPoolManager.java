package com.flowci.pool.manager;

import com.flowci.pool.DockerClientExecutor;
import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.DockerPoolException;
import com.flowci.util.UnixHelper;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.flowci.pool.domain.AgentContainer.NameFilter;
import static com.flowci.pool.domain.AgentContainer.name;
import static com.flowci.pool.domain.StartContext.AgentEnvs.*;

@Log4j2
public class SocketPoolManager implements PoolManager<SocketInitContext> {

    private DockerClientExecutor executor;

    /**
     * Init local docker.sock api interface
     */
    @Override
    public void init(SocketInitContext context) {
        this.executor = context.getExecutor();
        if (Objects.isNull(this.executor)) {
            this.executor = new DockerClientExecutor();
        }
    }

    @Override
    public List<AgentContainer> list(Optional<String> state) throws DockerPoolException {
        ListContainersCmd cmd = executor.getClient().listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(NameFilter));

        state.ifPresent((s) -> cmd.withStatusFilter(Lists.newArrayList(state.get())));

        try {
            List<Container> list = executor.exec(cmd);
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
            ListContainersCmd cmd = executor.getClient().listContainersCmd()
                    .withShowAll(true)
                    .withNameFilter(Lists.newArrayList(NameFilter));
            return executor.exec(cmd).size();
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.close();
        }
    }

    @Override
    public void start(StartContext context) throws DockerPoolException {
        try {
            final String name = name(context.getAgentName());
            final Path srcDirOnHost = UnixHelper.replacePathWithEnv(context.getDirOnHost());

            CreateContainerCmd createCmd = executor.getClient().createContainerCmd(AgentContainer.Image).withName(name)
                    .withEnv(String.format("%s=%s", SERVER_URL, context.getServerUrl()),
                            String.format("%s=%s", AGENT_TOKEN, context.getToken()),
                            String.format("%s=%s", AGENT_LOG_LEVEL, context.getLogLevel()),
                            String.format("%s=%s", AGENT_WORKSPACE, StartContext.DefaultWorkspace),
                            String.format("%s=%s", AGENT_VOLUMES, System.getenv(AGENT_VOLUMES)))
                    .withBinds(
                            new Bind(srcDirOnHost.toString(), new Volume(StartContext.DefaultWorkspace)),
                            new Bind(StartContext.DockerSock, new Volume(StartContext.DockerSock)));
            CreateContainerResponse container = executor.exec(createCmd);

            StartContainerCmd startCmd = executor.getClient().startContainerCmd(container.getId());
            executor.exec(startCmd);
        } catch (Exception e) {
            throw new DockerPoolException(e.getMessage());
        }
    }

    @Override
    public void stop(String name) throws DockerPoolException {
        try {
            StopContainerCmd stopCmd = executor.getClient().stopContainerCmd(findContainer(name).getId());
            executor.exec(stopCmd);
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void resume(String name) throws DockerPoolException {
        try {
            Container c = findContainer(name);
            StartContainerCmd startCmd = executor.getClient().startContainerCmd(c.getId());
            executor.exec(startCmd);
        } catch (DockerException e) {
            throw new DockerPoolException(e);
        }
    }

    @Override
    public void remove(String name) throws DockerPoolException {
        try {
            RemoveContainerCmd removeCmd = executor.getClient().removeContainerCmd(findContainer(name).getId()).withForce(true);
            executor.exec(removeCmd);
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
        try {
            String containerName = name(name);

            ListContainersCmd listCmd = executor.getClient().listContainersCmd()
                    .withShowAll(true)
                    .withNameFilter(Lists.newArrayList(containerName));

            List<Container> list = executor.exec(listCmd);

            if (list.size() != 1) {
                throw new DockerPoolException("Unable to find container for agent {0}", containerName);
            }

            return list.get(0);
        } catch (Exception e) {
            throw new DockerPoolException(e.getMessage());
        }
    }
}