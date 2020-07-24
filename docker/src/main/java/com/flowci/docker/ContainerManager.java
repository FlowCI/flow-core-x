package com.flowci.docker;

import com.flowci.docker.domain.DockerStartOption;
import com.github.dockerjava.api.model.Container;

import java.util.List;

public interface ContainerManager {

    List<Container> list(List<String> statusFilter, List<String> nameFilter) throws Exception;

    String start(DockerStartOption option) throws Exception;

    void stop(String containerId) throws Exception;

    void resume(String containerId) throws Exception;

    void delete(String containerId) throws Exception;
}
