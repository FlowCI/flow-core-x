package com.flowci.tree;

import com.flowci.tree.yml.FlowYml;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public final class ParallelStepNode extends Node implements StepNode {

    private Map<String, FlowYml> parallel;

    private int order;

    public ParallelStepNode(String name, Node parent) {
        super(name, parent);
    }
}
