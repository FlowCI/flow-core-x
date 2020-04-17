/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.tree.yml;

import com.flowci.exception.YmlException;
import com.flowci.tree.*;

import java.util.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
public class FlowYml extends YmlBase<FlowNode> {

    private String cron;

    @NonNull
    private Selector selector = new Selector();

    @NonNull
    private TriggerFilter trigger = new TriggerFilter();

    @NonNull
    private List<StepYml> steps = new LinkedList<>();

    @NonNull
    private List<StepYml> after = new LinkedList<>();

    public FlowYml(FlowNode node) {
        setEnvs(node.getEnvironments());

        // set children
        for (StepNode child : node.getChildren()) {
            this.steps.add(new StepYml(child));
        }
    }

    @Override
    public FlowNode toNode(int ignore) {
        if (!NodePath.validate(name)) {
            throw new YmlException("Invalid name {0}", name);
        }

        FlowNode node = new FlowNode(name);
        node.setCron(cron);
        node.setSelector(selector);
        node.setTrigger(trigger);
        node.setEnvironments(getVariableMap());

        setupSteps(node);
        setupAfter(node);
        return node;
    }

    private void setupAfter(FlowNode node) {
        if (Objects.isNull(after) || after.isEmpty()) {
            return;
        }

        int index = 1;
        for (StepYml child : after) {
            node.getAfter().add(child.toNode(index));
        }
    }

    private void setupSteps(FlowNode node) {
        if (Objects.isNull(steps) || steps.isEmpty()) {
            throw new YmlException("The 'steps' must be defined");
        }

        int index = 1;
        Set<String> uniqueName = new HashSet<>(steps.size());

        for (StepYml child : steps) {
            StepNode step = child.toNode(index++);

            if (!uniqueName.add(step.getName())) {
                throw new YmlException("Duplicate step name {0}", step.getName());
            }

            node.getChildren().add(step);
        }
    }
}
