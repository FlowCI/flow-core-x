package com.flowci.tree;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public abstract class ParentNode extends ConfigNode {

    /**
     * Children steps
     */
    protected List<StepNode> children = new LinkedList<>();

    public ParentNode(String name, Node parent) {
        super(name, parent);
    }

    @JsonIgnore
    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
