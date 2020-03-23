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

import com.flowci.tree.TriggerFilter;
import com.flowci.tree.Node;
import com.flowci.tree.Selector;
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
public class FlowNode extends YmlNode {

    private String cron;

    @NonNull
    private Selector selector = new Selector();

    @NonNull
    private TriggerFilter trigger = new TriggerFilter();

    @NonNull
    private List<StepNode> steps = new LinkedList<>();

    public FlowNode(Node node) {
        setEnvs(node.getEnvironments());

        // set children
        for (Node child : node.getChildren()) {
            this.steps.add(new StepNode(child));
        }
    }

    @Override
    public Node toNode(int index) {
        Node node = new Node(getName());
        node.setCron(cron);
        node.setSelector(selector);
        node.setTrigger(trigger);
        node.setEnvironments(getVariableMap());
        setupChildren(node);
        return node;
    }

    private void setupChildren(Node root) {
        int index = 1;
        for (StepNode child : steps) {
            root.getChildren().add(child.toNode(index++));
        }
    }
}
