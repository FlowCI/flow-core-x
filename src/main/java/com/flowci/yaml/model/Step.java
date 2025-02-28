package com.flowci.yaml.model;

import java.util.List;

public interface Step<D extends Docker, S extends Step<D, S, C>, C extends Command> extends Base<D> {

    String getName();

    Boolean getAllowFailure();

    Integer getRetry();

    List<String> getDependsOn();

    List<C> getCommands();

    List<S> getNext();
}
