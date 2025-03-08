package com.flowci.yaml.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.yaml.model.Flow;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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
    public List<StepV2> getNext() {
        return stepsWithoutDependencies();
    }

    public List<StepV2> getNext(String stepName) {
        var step = stepNameMapping.get(stepName);
        return step == null ? Collections.emptyList() : step.getNext();
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
