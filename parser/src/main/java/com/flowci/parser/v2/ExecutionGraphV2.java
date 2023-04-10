package com.flowci.parser.v2;

import com.flowci.domain.node.FlowNode;
import com.flowci.domain.node.Node;
import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;
import com.flowci.exception.CIException;
import com.flowci.parser.ExecutionGraph;
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

        root = new GraphNode(flowNode);

        createGraphNodeMap(root);
        buildGraph(root);
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

    private void createGraphNodeMap(GraphNode root) {
        for (Node step : root.getNode().getSteps()) {
            NodePath stepPath = NodePath.create(root.getPath(), step.getName());
            step.setPath(stepPath);

            var graphNode = new GraphNode(step);
            if (graphNodeMap.containsKey(graphNode.getName())) {
                throw new CIException("Duplicate step name: " + graphNode.getName());
            }
            graphNodeMap.put(graphNode.getName(), graphNode);

            createGraphNodeMap(graphNode);
        }
    }

    private void buildGraph(GraphNode root) {
        List<StepNode> steps = root.getNode().getSteps();

        for (Node step : steps) {
            var stepGraphNode = graphNodeMap.get(step.getName());

            if (step instanceof StepNode stepNode) {
                var dependencies = stepNode.getDependencies();

                if (dependencies.isEmpty()) {
                    stepGraphNode.getParents().add(root);
                    root.getChildren().add(stepGraphNode);
                    root = stepGraphNode;
                    continue;
                }

                for (var depStepName : dependencies) {
                    var depGraphNode = graphNodeMap.get(depStepName);
                    if (depGraphNode == null) {
                        throw new CIException("Dependencies step {0} not defined", depStepName);
                    }

                    stepGraphNode.getParents().add(depGraphNode);
                    depGraphNode.getChildren().add(stepGraphNode);
                }
            }

            root = stepGraphNode;
        }
    }
}
