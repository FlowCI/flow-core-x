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

import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.exception.YmlException;
import com.flowci.parser.v1.Node;
import com.flowci.parser.v1.RegularStepNode;
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;


/**
 * @author yang
 */
@Setter
@Getter
public abstract class YmlBase<T extends Node> implements Serializable {

    public String name;

    public Map<String, String> envs = new LinkedHashMap<>();

    /**
     * Groovy script
     */
    public String condition;

    // either docker
    public DockerYml docker;

    // or dockers
    public List<DockerYml> dockers;

    public List<StepYml> steps = new LinkedList<>();

    protected StringVars getVariableMap() {
        StringVars variables = new StringVars(envs.size());
        variables.putAll(envs);
        return variables;
    }

    void setDockerToNode(T node) {
        if (hasDocker() && hasDockers()) {
            throw new YmlException("Only accept either 'docker' or 'dockers' section");
        }

        if (hasDocker()) {
            DockerOption option = docker.toDockerOption();
            option.setRuntime(true);
            node.getDockers().add(option);
            return;
        }

        if (hasDockers()) {
            int numOfRuntime = 0;
            DockerOption runtime = null;

            List<DockerOption> options = new ArrayList<>(dockers.size());
            for (DockerYml item : dockers) {
                DockerOption option = item.toDockerOption();
                options.add(option);

                if (option.isRuntime()) {
                    runtime = option;
                    numOfRuntime++;
                }
            }

            if (numOfRuntime == 1) {
                if (ObjectsHelper.hasCollection(runtime.getCommand())) {
                    throw new YmlException("'Command' section cannot be applied for runtime image");
                }

                node.getDockers().addAll(options);
                return;
            }

            if (numOfRuntime == 0) {
                throw new YmlException("The 'is_runtime' must be defined");
            }

            throw new YmlException("The 'is_runtime=true' can only be defined once");
        }
    }

    private boolean hasDocker() {
        return docker != null;
    }

    private boolean hasDockers() {
        return dockers != null && dockers.size() > 0;
    }

    protected static <T extends Node> void setStepsToParent(T parent, List<StepYml> steps, boolean post, Set<String> nameSet) {
        int index = 1;
        for (StepYml child : steps) {
            Node step = child.toNode(parent, index++);

            if (step instanceof RegularStepNode) {
                String stepName = step.getName();
                if (!nameSet.add(stepName)) {
                    throw new YmlException("Duplicate name {0} in step", stepName);
                }
                ((RegularStepNode) step).setPost(post);
            }

            parent.getChildren().add(step);
        }
    }
}
