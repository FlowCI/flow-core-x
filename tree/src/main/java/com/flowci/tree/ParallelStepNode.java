package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public final class ParallelStepNode extends Node {

    private Map<String, FlowNode> parallel = new LinkedHashMap<>();

    public ParallelStepNode(String name, Node parent) {
        super(name, parent);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>(parallel.size());
        parallel.forEach((k, v) -> children.add(v));
        return children;
    }
}
