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

package com.flowci.parser.v1.yml;

import com.flowci.exception.YmlException;
import com.flowci.parser.v1.FlowNode;
import com.flowci.parser.v1.Node;
import com.flowci.domain.tree.NodePath;
import com.flowci.parser.v1.Selector;
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

import static com.flowci.util.ObjectsHelper.*;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
public class FlowYml extends YmlBase<FlowNode> {

    private Selector selector;

    // post steps
    private List<StepYml> post = new LinkedList<>();

    /**
     * Merge yml element from others
     * it will through YMLException if elements are duplicated
     */
    @SneakyThrows
    public void merge(FlowYml other) {
        try {
            ObjectsHelper.merge(other, this);
        } catch (DuplicateFormatFlagsException e) {
            throw new YmlException(e.getMessage());
        }
    }

    public FlowNode toNode(Node parent) {
        if (!NodePath.validate(name)) {
            throw new YmlException("Invalid name {0}", name);
        }

        FlowNode node = new FlowNode(name, parent);
        node.setSelector(selector == null ? Selector.EMPTY : selector);
        node.setCondition(condition);
        node.setEnvironments(getVariableMap());

        setDockerToNode(node);

        if (!hasCollection(steps)) {
            throw new YmlException("The 'steps' section must be defined");
        }

        Set<String> uniqueNames = new HashSet<>(steps.size() + post.size());
        setStepsToParent(node, steps, false, uniqueNames);
        setStepsToParent(node, post, true, uniqueNames);
        return node;
    }
}
