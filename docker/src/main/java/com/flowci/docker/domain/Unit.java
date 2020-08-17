package com.flowci.docker.domain;

/**
 * Represent Container or Pod, the minimum unit of docker or k8s
 */
public interface Unit {

    /**
     * Unit id
     */
    String getId();

    /**
     * Name of unit
     */
    String getName();

    /**
     * Unit status
     */
    String getStatus();
}
