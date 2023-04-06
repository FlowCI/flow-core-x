package com.flowci.parser.v2.yml;

import com.flowci.domain.tree.NodePath;
import com.flowci.domain.tree.StepNode;
import com.flowci.exception.YmlException;
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

    protected List<DockerOptionYml> dockers = new LinkedList<>();

    protected List<String> agents = new LinkedList<>();

    /**
     * Max 2 levels
     * flow:
     * steps:
     * steps:
     */
    protected Map<String, StepYml> steps = new LinkedHashMap<>();

    protected List<StepNode> toStepNodeList() {
        return steps.entrySet().stream().map(entry -> {
            String name = entry.getKey();

            if (!NodePath.validate(name)) {
                throw new YmlException("Invalid name '{0}'", name);
            }

            StepNode node = entry.getValue().convert();
            node.setName(name);
            return node;
        }).toList();
    }
}
