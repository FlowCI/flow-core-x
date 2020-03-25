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

import com.flowci.tree.*;

import java.util.LinkedList;
import java.util.List;
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

    public FlowYml(FlowNode node) {
        setEnvs(node.getEnvironments());

        // set children
        for (StepNode child : node.getChildren()) {
            this.steps.add(new StepYml(child));
        }
    }

    @Override
    public FlowNode toNode(int index) {
        FlowNode node = new FlowNode(name);
        node.setCron(cron);
        node.setSelector(selector);
        node.setTrigger(trigger);
        node.setEnvironments(getVariableMap());
        setupChildren(node);
        return node;
    }

    private void setupChildren(FlowNode flow) {
        int index = 1;
        for (StepYml child : steps) {
            flow.getChildren().add(child.toNode(index++));
        }
    }
}
