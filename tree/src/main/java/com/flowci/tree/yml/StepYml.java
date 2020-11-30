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
import com.flowci.tree.Cache;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.StepNode;
import com.flowci.util.FileHelper;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * @author yang
 */
@Setter
@Getter
@NoArgsConstructor
public class StepYml extends YmlBase<StepNode> {

    private static final String DefaultStepPrefix = "step-";

    private String bash; // bash script

    private String pwsh; // powershell script

    private String script; // keep it to compatible old yaml

    private String plugin;

    private Integer retry; // num of retry

    private Integer timeout; // timeout in seconds

    private List<String> exports = new LinkedList<>();

    private Boolean allow_failure;

    private Cache cache;

    /**
     * Only for parallel step, other fields will not valid
     */
    private Map<String, FlowYml> parallel;

    public StepNode toNode(Node parent, int index) {
        if (parallel != null) {
            // TODO: handle parallel when field defined
        }

        StepNode node = new StepNode(buildName(index), parent);
        node.setCondition(condition);
        node.setBash(bash);
        node.setPwsh(pwsh);
        node.setRetry(retry);
        node.setTimeout(timeout);
        node.setPlugin(plugin);
        node.setExports(Sets.newHashSet(exports));
        node.setAllowFailure(allow_failure != null && allow_failure);
        node.setEnvironments(getVariableMap());

        setCacheToNode(node);
        setDockerToNode(node);

        if (StringHelper.hasValue(node.getName()) && !NodePath.validate(node.getName())) {
            throw new YmlException("Invalid name '{0}'", node.getName());
        }

        if (ObjectsHelper.hasCollection(steps)) {
            if (node.hasPlugin()) {
                throw new YmlException("The plugin section is not allowed on the step with sub steps");
            }

            setStepsToNode(node);
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

    /**
     * set cache from yaml
     * read only cache if path not specified
     */
    private void setCacheToNode(StepNode node) {
        if (Objects.isNull(cache)) {
            return;
        }

        if (!StringHelper.hasValue(cache.getKey())) {
            throw new YmlException("Cache key must be defined");
        }

        if (!NodePath.validate(cache.getKey())) {
            throw new YmlException("Invalid cache key {0}", cache.getKey());
        }

        if (!ObjectsHelper.hasCollection(cache.getPaths())) {
            cache.setPaths(Collections.emptyList());
        }

        for (String path : cache.getPaths()) {
            if (FileHelper.isStartWithRoot(path)) {
                throw new YmlException("Cache path cannot be defined as absolute path");
            }
        }

        if (FileHelper.hasOverlapOrDuplicatePath(cache.getPaths())) {
            throw new YmlException("Cache paths are overlap or duplicate");
        }

        Cache c = new Cache();
        c.setKey(cache.getKey());
        c.setPaths(cache.getPaths());

        node.setCache(c);
    }
}
