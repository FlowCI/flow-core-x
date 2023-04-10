package com.flowci.parser.v2;

import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;
import com.flowci.exception.CIException;
import com.flowci.parser.ExecutionGraph;
import com.flowci.parser.v2.graph.GraphNode;
import com.flowci.parser.v2.yml.FlowYml;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ExecutionGraphV2 implements ExecutionGraph {

    private final Map<String, GraphNode> graphNodeMap = new LinkedHashMap<>(); // name , GroupNode map

    private final GraphNode root;

    public ExecutionGraphV2(FlowYml flowYml) {
        var flowNode = flowYml.convert();
        flowNode.setPath(NodePath.create(flowNode.getName()));

        this.root = new GraphNode(flowNode);

        createStepGraphNodeMap(this.root);
        buildGraph(this.root);
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

    private void createStepGraphNodeMap(GraphNode root) {
        for (StepNode step : root.getNode().getSteps()) {
            NodePath stepPath = NodePath.create(root.getPath(), step.getName());
            step.setPath(stepPath);

            var stepGraphNode = new GraphNode(step);
            if (graphNodeMap.containsKey(stepGraphNode.getName())) {
                throw new CIException("Duplicate step name: " + stepGraphNode.getName());
            }
            graphNodeMap.put(stepGraphNode.getName(), stepGraphNode);

            createStepGraphNodeMap(stepGraphNode);
        }
    }

    private void buildGraph(GraphNode root) {
        List<StepNode> steps = root.getNode().getSteps();

        for (StepNode sn : steps) {
            var stepGraphNode = graphNodeMap.get(sn.getName());
            var dependencies = sn.getDependencies();

            try {
                if (dependencies.isEmpty()) {
                    stepGraphNode.getParents().add(root);
                    root.getChildren().add(stepGraphNode);
                    continue;
                }

                for (var depStepName : dependencies) {
                    var depGraphNode = graphNodeMap.get(depStepName);

                    if (depGraphNode == null) {
                        throw new CIException("Dependencies step {0} not defined", depStepName);
                    }

                    if (depGraphNode.isStage()) {
                        depGraphNode = getLastNodeOnStage(depGraphNode);
                    }

                    stepGraphNode.getParents().add(depGraphNode);
                    depGraphNode.getChildren().add(stepGraphNode);
                }
            } finally {
                root = sn.isStage() ? buildGraphOnStage(stepGraphNode) : stepGraphNode;
            }
        }
    }

    private GraphNode getLastNodeOnStage(GraphNode stage) {
        List<StepNode> steps = stage.getNode().getSteps();
        GraphNode last = null;
        for (StepNode sn : steps) {
            last = graphNodeMap.get(sn.getName());
        }
        return last;
    }

    private GraphNode buildGraphOnStage(GraphNode root) {
        List<StepNode> steps = root.getNode().getSteps();

        for (StepNode sn : steps) {
            var stepGraphNode = graphNodeMap.get(sn.getName());

            stepGraphNode.getParents().add(root);
            root.getChildren().add(stepGraphNode);

            root = stepGraphNode;
        }

        return root;
    }
}
