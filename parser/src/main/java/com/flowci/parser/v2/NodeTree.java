package com.flowci.parser.v2;

import com.flowci.domain.node.FlowNode;
import com.flowci.domain.node.Node;
import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;
import com.flowci.parser.Tree;
import lombok.Getter;

import java.util.Collection;

@Getter
public class NodeTree implements Tree {

    private final FlowNode root;

    public NodeTree(FlowNode root) {
        this.root = root;
    }

    @Override
    public StepNode find(String path) {
        return null;
    }

    @Override
    public StepNode find(NodePath path) {
        return null;
    }

    @Override
    public Collection<StepNode> next(StepNode current, boolean post) {
        return null;
    }

    @Override
    public Collection<StepNode> skip(StepNode current) {
        return null;
    }

    @Override
    public Collection<StepNode> previous(StepNode current) {
        return null;
    }

    private void buildGraph(Node flow) {

    }
}
