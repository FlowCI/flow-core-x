package com.flowci.docker.domain;

/**
 * Represent Container or Pod, the minimum unit of docker or k8s
 */
public interface Unit {

    String getId();

    String getName();

    String getStatus();

    Long getExitCode();

    Boolean isRunning();
}
