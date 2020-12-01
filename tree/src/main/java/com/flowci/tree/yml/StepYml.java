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
public class StepYml extends YmlBase<RegularStepNode> {

    private static final String DefaultStepPrefix = "step-";

    private static final String DefaultParallelPrefix = "parallel-";

    private static final Set<String> FieldsForStep = ObjectsHelper.fields(StepYml.class);

    static {
        FieldsForStep.remove("parallel");
    }

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
            try {
                if (ObjectsHelper.hasValue(this, FieldsForStep)) {
                    throw new YmlException("Parallel section only");
                }
            } catch (ReflectiveOperationException e) {
                throw new YmlException(e.getMessage());
            }

            if (parallel.isEmpty()) {
                throw new YmlException("Parallel flow must be defined");
            }

            String name = DefaultParallelPrefix + index;
            ParallelStepNode step = new ParallelStepNode(name, parent);
            for (Map.Entry<String, FlowYml> entry : parallel.entrySet()) {
                String subflowName = entry.getKey();

                FlowYml yaml = entry.getValue();
                yaml.setName(subflowName);

                step.getParallel().put(subflowName, yaml.toNode());
            }
            return step;
        }

        RegularStepNode step = new RegularStepNode(buildName(index), parent);
        step.setCondition(condition);
        step.setBash(bash);
        step.setPwsh(pwsh);
        step.setRetry(retry);
        step.setTimeout(timeout);
        step.setPlugin(plugin);
        step.setExports(Sets.newHashSet(exports));
        step.setAllowFailure(allow_failure != null && allow_failure);
        step.setEnvironments(getVariableMap());

        setCacheToNode(step);
        setDockerToNode(step);

        if (StringHelper.hasValue(step.getName()) && !NodePath.validate(step.getName())) {
            throw new YmlException("Invalid name '{0}'", step.getName());
        }

        if (ObjectsHelper.hasCollection(steps)) {
            if (step.hasPlugin()) {
                throw new YmlException("The plugin section is not allowed on the step with sub steps");
            }

            setStepsToNode(step);
        }

        // backward compatible, set script to bash
        if (StringHelper.hasValue(script) && !StringHelper.hasValue(bash)) {
            step.setBash(script);
        }

        return step;
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
    private void setCacheToNode(RegularStepNode node) {
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
