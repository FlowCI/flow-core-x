package com.flowci.yaml.model;

import java.util.List;

public interface Flow<D extends Docker, S extends Step<D, S, C>, C extends Command> extends Base<D> {

    List<S> getSteps();

    List<S> getNext();

    S getStep(String name);
}
