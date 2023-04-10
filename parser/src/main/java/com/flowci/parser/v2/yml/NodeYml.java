package com.flowci.parser.v2.yml;

import com.flowci.domain.node.Node;
import com.flowci.domain.node.NodePath;
import com.flowci.domain.node.StepNode;
import com.flowci.exception.YmlException;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public abstract class NodeYml {

    protected final static int MaxStepDepth = 2;

    protected Map<String, String> vars = new LinkedHashMap<>();

    protected String condition;

    protected List<DockerOptionYml> dockers = new LinkedList<>();

    protected List<String> agents = new LinkedList<>();

    /**
     * Max 2 levels
     * flow:
     * steps:
     * steps:
     */
    protected Map<String, StepYml> steps = new LinkedHashMap<>();

    protected List<StepNode> toStepNodeList(Node parent, int depth) {
        if (steps.isEmpty()) {
            return Collections.emptyList();
        }

        if (depth > MaxStepDepth) {
            throw new YmlException("Max step depth is 2");
        }

        return steps.entrySet().stream().map(entry -> {
            String name = entry.getKey();

            if (!NodePath.validate(name)) {
                throw new YmlException("Invalid name '{0}'", name);
            }

            return entry.getValue().convert(parent, name, depth);
        }).toList();
    }
}
