package com.flowci.yaml.business.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flowci.common.validator.ValidName;
import com.flowci.yaml.business.ParseYaml;
import com.flowci.yaml.exception.InvalidYamlException;
import com.flowci.yaml.model.Step;
import com.flowci.yaml.model.v2.FlowV2;
import com.flowci.yaml.model.v2.StepV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
public class ParseYamlV2Impl implements ParseYaml {

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private static final ValidName.NameValidator nameValidator = new ValidName.NameValidator();

    static {
        objectMapper.findAndRegisterModules();
    }

    @Override
    public FlowV2 invoke(String yaml) {
        try {
            var flowV2 = objectMapper.readValue(yaml, FlowV2.class);
            for (var step : flowV2.getSteps()) {
                if (step instanceof StepV2 v2) {
                    v2.setParent(flowV2);
                }
            }

            validateSteps(flowV2);
            buildGraph(flowV2);
            return flowV2;
        } catch (JsonProcessingException e) {
            log.error("invalid YAML configuration", e);
            throw new InvalidYamlException("invalid YAML configuration");
        }
    }

    private void validateSteps(FlowV2 flow) {
        var steps = flow.getSteps();
        if (isEmpty(steps)) {
            throw new InvalidYamlException("at least one step is required");
        }

        var stepNameSet = new HashSet<>(steps.size());
        for (var step : steps) {
            if (!stepNameSet.add(step.getName())) {
                throw new InvalidYamlException(format("step name '%s' already exists", step.getName()));
            }
        }

        for (var step : steps) {
            if (!nameValidator.isValid(step.getName(), null)) {
                throw new InvalidYamlException(format("step name '%s' is invalid", step.getName()));
            }

            if (!isEmpty(step.getDependsOn())) {
                for (var dependsOn : step.getDependsOn()) {
                    if (!stepNameSet.contains(dependsOn)) {
                        throw new InvalidYamlException(format("depends on '%s' is not found", dependsOn));
                    }
                }
            }

            validateCommands(step);
        }
    }

    private void validateCommands(StepV2 step) {
        var commands = step.getCommands();
        if (isEmpty(commands)) {
            throw new InvalidYamlException(format("at least one command under step '%s' is required", step.getName()));
        }

        for (var command : commands) {
            if (!hasText(command.getBash()) && !hasText(command.getPwsh())) {
                throw new InvalidYamlException("bash or powershell is required");
            }
        }
    }

    private void buildGraph(FlowV2 flow) {
        var steps = flow.getSteps();

        // set name - step mapping
        var map = new HashMap<String, StepV2>(steps.size());
        flow.setStepNameMapping(map);

        for (var step : steps) {
            map.put(step.getName(), step);
        }

        for (var step : steps) {
            if (isEmpty(step.getDependsOn())) {
                continue;
            }

            for (var dep : step.getDependsOn()) {
                var depStep = map.get(dep);

                // link next
                depStep.getNext().add(step);
            }
        }

        for (var step : steps) {
            checkCircularDependencies(List.of(step), new HashMap<>());
        }
    }

    private void checkCircularDependencies(List<Step> steps, HashMap<Step, Integer> traversed) {
        for (var step : steps) {
            Integer numEncountered = traversed.get(step);

            if (numEncountered == null) {
                traversed.put(step, 1);
                checkCircularDependencies(step.getNext(), traversed);
                continue;
            }

            int numOfDepends = step.getDependsOn() == null ? 0 : step.getDependsOn().size();
            if (numOfDepends == 0 || numEncountered > numOfDepends) {
                throw new InvalidYamlException("circular dependency found on step: " + step.getName());
            }

            traversed.put(step, numEncountered + 1);
            checkCircularDependencies(step.getNext(), traversed);
        }
    }
}
