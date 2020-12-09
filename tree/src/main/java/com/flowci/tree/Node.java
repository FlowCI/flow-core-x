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
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

    public String getPathAsString() {
        return path.getPathInStr();
    }

    @JsonIgnore
    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    @JsonIgnore
    public boolean hasParent() {
        return parent != null;
    }

    public String getEnv(String name) {
        return environments.get(name);
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
    public StringVars allEnvs() {
        return collectionAllEnvs(this);
    }

    public void forEachChildren(Consumer<Node> onChild) {
        forEachChildren(this, onChild);
    }

    private void forEachChildren(Node current, Consumer<Node> onChild) {
        for (Node child : current.getChildren()) {
            onChild.accept(child);
            forEachChildren(child, onChild);
        }
    }

    private StringVars collectionAllEnvs(Node node) {
        StringVars output = new StringVars(environments);

        Node parent = node.getParent();
        if (parent != null) {
            output.merge(collectionAllEnvs(parent));
        }

        return output;
    }
}
