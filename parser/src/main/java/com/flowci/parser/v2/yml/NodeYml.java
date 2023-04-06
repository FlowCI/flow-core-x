package com.flowci.parser.v2.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class NodeYml {

    protected Map<String, String> vars = new LinkedHashMap<>();

    protected String condition;

    protected DockerOptionYml docker;

    protected List<String> agents = new LinkedList<>();

    /**
     * Max 2 levels
     * flow:
     *  steps:
     *    steps:
     */
    protected Map<String, StepYml> steps = new LinkedHashMap<>();
}
