package com.flowci.docker.domain;

public interface Inspected {

    Long getExitCode();

    Boolean isRunning();
}
