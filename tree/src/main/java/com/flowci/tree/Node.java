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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"path"})
@ToString(of = {"path"})
public abstract class Node implements Nodeable, Serializable {

    public static final int DEFAULT_ORDER = -1;

    protected String name;

    protected NodePath path;

    @JsonIgnore
    protected Node parent;

    protected int order = DEFAULT_ORDER;

    protected int nextOrder = DEFAULT_ORDER;

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

    @JsonIgnore
    public boolean hasParent() {
        return parent != null;
    }
}
