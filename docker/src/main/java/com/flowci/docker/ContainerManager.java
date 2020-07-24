package com.flowci.docker;

import com.flowci.docker.domain.DockerStartOption;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;

import java.util.List;
import java.util.function.Consumer;

public interface ContainerManager {

    List<Container> list(List<String> statusFilter, List<String> nameFilter) throws Exception;

    InspectContainerResponse inspect(String containerId) throws Exception;

    String start(DockerStartOption option) throws Exception;

    void wait(String containerId, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception;

    void stop(String containerId) throws Exception;

    void resume(String containerId) throws Exception;

    void delete(String containerId) throws Exception;
}
