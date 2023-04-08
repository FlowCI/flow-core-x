package com.flowci.parser.v2;

import com.flowci.domain.node.FlowNode;
import com.flowci.domain.node.Node;
import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;
import com.flowci.parser.FlowGraph;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
public class FlowGraphV2 implements FlowGraph {

    private final Map<String, Node> nodePathMap = new HashMap<>();

    private final FlowNode root;

    public FlowGraphV2(FlowNode root) {
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

    private void createNodeMap(Node root) {
        for (Node step : root.getSteps()) {

        }
    }

    private void buildGraph(Node root) {

    }
}
