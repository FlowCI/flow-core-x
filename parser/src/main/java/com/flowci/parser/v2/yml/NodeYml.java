package com.flowci.parser.v2.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class NodeYml {

    protected Map<String, String> vars;

    protected String condition;

    protected DockerOptionYml docker;

    protected List<String> agents;

    /**
     * Max 2 levels
     * flow:
     *  steps:
     *    steps:
     */
    protected Map<String, StepYml> steps;
}
