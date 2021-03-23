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
import com.flowci.domain.ObjectWrapper;
import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"path"})
@ToString(of = {"path"})
public abstract class Node implements Serializable {

    /**
     * Node name
     */
    protected String name;

    /**
     * Node path
     */
    protected NodePath path;

    /**
     * Parent node from tree
     */
    @JsonIgnore
    protected Node parent;

    /**
     * Node before groovy script;
     */
    protected String condition;

    /**
     * Inner option has higher priority
     * Ex: Plugin > Step > Flow
     */
    protected List<DockerOption> dockers = new LinkedList<>();

    /**
     * Input environment variables
     */
    protected StringVars environments = new StringVars();

    /**
     * Previous node list
     */
    @JsonIgnore
    protected List<Node> prev = new LinkedList<>();

    /**
     * Next node list
     */
    @JsonIgnore
    protected List<Node> next = new LinkedList<>();

    public Node(String name, Node parent) {
        this.name = name;
        this.parent = parent;

        if (Objects.isNull(parent)) {
            this.path = NodePath.create(name);
            return;
        }

        this.path = NodePath.create(parent.getPath(), name);
    }

    public abstract List<Node> getChildren();

    public boolean isLastChildOfParent() {
        if (parent == null) {
            return false;
        }

        List<Node> children = this.parent.getChildren();
        if (children.isEmpty()) {
            return false;
        }

        return children.get(children.size() - 1).equals(this);
    }

    public String getPathAsString() {
        return path.getPathInStr();
    }

    public String getEnv(String name) {
        return environments.get(name);
    }

    @JsonIgnore
    public <T extends Node> T getParent(Class<T> klass) {
        ObjectWrapper<T> wrapper = new ObjectWrapper<>();
        this.forEachBottomUp(this, (n) -> {
            if (klass.isInstance(n)) {
                wrapper.setValue((T) n);
                return false;
            }
            return true;
        });
        return wrapper.getValue();
    }

    @JsonIgnore
    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    @JsonIgnore
    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }

    @JsonIgnore
    public boolean hasDocker() {
        return !dockers.isEmpty();
    }

    @JsonIgnore
    public StringVars fetchEnvs() {
        StringVars output = new StringVars();

        this.forEachBottomUp(this, (n) -> {
            output.merge(n.getEnvironments(), false);
            return true;
        });

        return output;
    }

    @JsonIgnore
    public List<DockerOption> fetchDockerOptions() {
        ObjectWrapper<List<DockerOption>> wrapper = new ObjectWrapper<>(Collections.emptyList());

        this.forEachBottomUp(this, (n) -> {
            if (n.hasDocker()) {
                wrapper.setValue(n.getDockers());
                return false;
            }
            return true;
        });

        return wrapper.getValue();
    }

    protected final void forEachBottomUp(Node node, Function<Node, Boolean> onNode) {
        Boolean canContinue = onNode.apply(node);
        if (!canContinue) {
            return;
        }

        Node parent = node.getParent();
        if (parent != null) {
            forEachBottomUp(parent, onNode);
        }
    }
}
