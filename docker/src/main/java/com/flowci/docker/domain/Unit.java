package com.flowci.docker.domain;

/**
 * Represent container or pod
 */
public interface Unit {

    String getId();

    String getName();
}
