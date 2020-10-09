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
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.StepNode;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yang
 */
@Setter
@Getter
@NoArgsConstructor
public class StepYml extends YmlBase<StepNode> {

    private static final String DefaultStepPrefix = "step-";

    /**
     * Groovy script
     */
    private String condition;

    private String bash; // bash script

    private String pwsh; // powershell script

    private String script; // keep it to compatible old yaml

    private String plugin;

    private Integer timeout; // timeout in seconds

    private List<String> exports = new LinkedList<>();

    private boolean allow_failure = false;

    StepYml(StepNode node) {
        setName(node.getName());
        setEnvs(node.getEnvironments());
        setBash(node.getBash());
        setPwsh(node.getPwsh());
        setTimeout(node.getTimeout());
        setPlugin(node.getPlugin());
        setAllow_failure(node.isAllowFailure());
    }

    public StepNode toNode(Node parent, int index) {
        StepNode node = new StepNode(buildName(index), parent);
        node.setCondition(condition);
        node.setBash(bash);
        node.setPwsh(pwsh);
        node.setTimeout(timeout);
        node.setPlugin(plugin);
        node.setExports(Sets.newHashSet(exports));
        node.setAllowFailure(allow_failure);
        node.setEnvironments(getVariableMap());
        setDocker(node);

        if (StringHelper.hasValue(node.getName()) && !NodePath.validate(node.getName())) {
            throw new YmlException("Invalid name '{0}'", node.getName());
        }

        if (ObjectsHelper.hasCollection(steps)) {
            if (node.hasPlugin()) {
                throw new YmlException("The plugin section is not allowed on the step with sub steps");
            }

            setSteps(node);
        }

        // backward compatible, set script to bash
        if (StringHelper.hasValue(script) && !StringHelper.hasValue(bash)) {
            node.setBash(script);
        }

        return node;
    }

    private String buildName(int index) {
        if (StringHelper.hasValue(name)) {
            return name;
        }

        return DefaultStepPrefix + index;
    }
}
