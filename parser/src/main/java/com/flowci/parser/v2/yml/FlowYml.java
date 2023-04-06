package com.flowci.parser.v2.yml;

import com.flowci.domain.StringVars;
import com.flowci.domain.tree.FlowNode;
import com.flowci.domain.tree.StepNode;
import com.flowci.exception.YmlException;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.DuplicateFormatFlagsException;

@Getter
@Setter
public class FlowYml extends NodeYml implements Convertable<FlowNode> {

    public static final String DEFAULT_NAME = "root";

    private String name;

    private Integer version;

    @SneakyThrows
    public void merge(FlowYml other) {
        try {
            ObjectsHelper.merge(other, this);
        } catch (DuplicateFormatFlagsException e) {
            throw new YmlException(e.getMessage());
        }
    }

    @Override
    public FlowNode convert() {
        return FlowNode.builder()
                .name(StringHelper.hasValue(name) ? name : DEFAULT_NAME)
                .vars(new StringVars(vars))
                .condition(condition)
                .docker(docker == null ? null : docker.convert())
                .agents(agents)
                .steps(steps.entrySet().stream().map(entry -> {
                    StepNode node = entry.getValue().convert();
                    node.setName(entry.getKey());
                    return node;
                }).toList())
                .build();
    }
}
