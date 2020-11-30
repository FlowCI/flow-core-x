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
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
public class FlowYml extends YmlBase<FlowNode> {

    private Selector selector = new Selector();

    private List<NotifyYml> notifications = new LinkedList<>();

    public FlowNode toNode() {
        if (!NodePath.validate(name)) {
            throw new YmlException("Invalid name {0}", name);
        }

        FlowNode node = new FlowNode(name);
        node.setSelector(selector);
        node.setCondition(condition);
        node.setEnvironments(getVariableMap());;

        setDockerToNode(node);
        setupNotifications(node);

        if (!ObjectsHelper.hasCollection(steps)) {
            throw new YmlException("The 'steps' section must be defined");
        }

        setStepsToNode(node);
        return node;
    }

    private void setupNotifications(FlowNode node) {
        if (notifications.isEmpty()) {
            return;
        }

        Set<String> uniqueName = new HashSet<>(notifications.size());
        for (NotifyYml n : notifications) {
            if (!uniqueName.add(n.getPlugin())) {
                throw new YmlException("Duplicate plugin {0} defined in notifications", n.getPlugin());
            }

            node.getNotifications().add(n.toObj());
        }
    }
}
