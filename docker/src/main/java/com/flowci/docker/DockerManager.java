package com.flowci.docker;

import com.flowci.docker.domain.StartOption;

public interface DockerManager {

    String DockerLocalHost = "unix:///var/run/docker.sock";

    ContainerManager getContainerManager();

    ImageManager getImageManager();

    void close();
}
