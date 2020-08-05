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

package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"path"})
@ToString(of = {"path"})
public abstract class Node implements Serializable {

    protected String name;

    protected NodePath path;

    /**
     * Parent could be FlowNode or StepNode
     */
    protected Node parent;

    /**
     * Inner option has higher priority
     * Ex: Plugin > Step > Flow
     */
    protected List<DockerOption> dockers = new LinkedList<>();

    protected StringVars environments = new StringVars();

    protected List<StepNode> children = new LinkedList<>();

    public Node(String name, Node parent) {
        this.name = name;
        this.parent = parent;

        if (Objects.isNull(parent)) {
            this.path = NodePath.create(name);
            return;
        }

        this.path = NodePath.create(parent.getPath(), name);
    }

    public String getPathAsString() {
        return path.getPathInStr();
    }

    public String getEnv(String name) {
        return environments.get(name);
    }

    @JsonIgnore
    public boolean hasDocker() {
        return !dockers.isEmpty();
    }

    @JsonIgnore
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @JsonIgnore
    public boolean hasParent() {
        return parent != null;
    }
}
