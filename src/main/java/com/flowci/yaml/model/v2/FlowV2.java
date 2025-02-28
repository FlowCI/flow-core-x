package com.flowci.yaml.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.yaml.model.Flow;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@Setter
@Getter
public class FlowV2 extends BaseV2 implements Flow<DockerV2, StepV2, CommandV2> {

    /**
     * List of agent tags
     */
    private Set<String> agents;

    private List<StepV2> steps;

    @JsonIgnore
    private Map<String, StepV2> stepNameMapping;

    @Override
    @JsonIgnore
    public StepV2 getStep(String name) {
        return stepNameMapping.get(name);
    }

    @Override
    @JsonIgnore
    public List<StepV2> next(@Nullable String current) {
        if (!StringUtils.hasText(current)) {
            return stepsWithoutDependencies();
        }

        var step = stepNameMapping.get(current);
        if (step == null) {
            throw new IllegalArgumentException("Invalid step name: " + current);
        }

        return step.getNext();
    }

    private List<StepV2> stepsWithoutDependencies() {
        var list = new LinkedList<StepV2>();
        for (var step : steps) {
            if (isEmpty(step.getDependsOn())) {
                list.add(step);
            }
        }
        return list;
    }
}
