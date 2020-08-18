package com.flowci.docker.domain;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.Getter;

@Getter
public class ContainerUnit implements Unit {

    private final String id;

    private final String name;

    private final String status;

    private final Long exitCode;

    private final Boolean running;

    public ContainerUnit(Container c) {
        this.id = c.getId();
        this.name = c.getNames()[0];
        this.status = c.getStatus();
        this.exitCode = null;
        this.running = null;
    }

    public ContainerUnit(InspectContainerResponse r) {
        this.id = r.getId();
        this.name = r.getName();
        this.status = r.getState().getStatus();
        this.exitCode = r.getState().getExitCodeLong();
        this.running = r.getState().getRunning();
    }

    @Override
    public Boolean isRunning() {
        return running;
    }
}
