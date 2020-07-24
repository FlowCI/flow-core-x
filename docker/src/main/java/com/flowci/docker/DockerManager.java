package com.flowci.docker;

public interface DockerManager {

    String DockerLocalHost = "unix:///var/run/docker.sock";

    ContainerManager getContainerManager();

    ImageManager getImageManager();
}
