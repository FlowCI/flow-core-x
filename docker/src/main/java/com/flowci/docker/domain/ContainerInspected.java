package com.flowci.docker.domain;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;

@Getter
public class ContainerInspected implements Inspected {

    private final InspectContainerResponse content;

    public ContainerInspected(InspectContainerResponse content) {
        this.content = content;
    }

    @Override
    public Long getExitCode() {
        return content.getState().getExitCodeLong();
    }

    @Override
    public Boolean isRunning() {
        return content.getState().getRunning();
    }
}
