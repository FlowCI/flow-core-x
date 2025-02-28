package com.flowci.yaml.model;

import jakarta.annotation.Nullable;

import java.util.List;

public interface Flow<D extends Docker, S extends Step<D, S, C>, C extends Command> extends Base<D> {

    List<S> getSteps();

    List<S> next(@Nullable String current);

    S getStep(String name);
}
