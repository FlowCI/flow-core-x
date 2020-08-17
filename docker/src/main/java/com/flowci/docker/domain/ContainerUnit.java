package com.flowci.docker.domain;

import com.github.dockerjava.api.model.Container;
import lombok.Getter;

@Getter
public class ContainerUnit implements Unit {

    private final Container container;

    public ContainerUnit(Container container) {
        this.container = container;
    }

    @Override
    public String getId() {
        return container.getId();
    }

    @Override
    public String getName() {
        return container.getNames()[0];
    }
}
