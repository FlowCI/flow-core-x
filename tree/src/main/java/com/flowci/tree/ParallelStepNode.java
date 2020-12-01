package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public final class ParallelStepNode extends ConfigNode implements StepNode {

    private Map<String, FlowNode> parallel = new LinkedHashMap<>();

    private int order;

    public ParallelStepNode(String name, Node parent) {
        super(name, parent);
    }
}
