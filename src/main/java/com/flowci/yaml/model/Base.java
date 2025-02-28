package com.flowci.yaml.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Base<D extends Docker> {

    Map<String, String> getVariables();

    // condition script , todo: python or groovy?
    String getCondition();

    Integer getTimeout();

    Set<String> getAgents();

    D getDocker();

    List<D> getDockers();
}
