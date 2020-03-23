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
import com.flowci.domain.StringVars;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"path"})
@ToString(of = {"path"})
public class Node implements Serializable {

    public final static boolean ALLOW_FAILURE_DEFAULT = false;

    public final static boolean IS_TAIL_DEFAULT = false;

    private String name;

    private NodePath path;

    private StringVars environments = new StringVars();

    /**
     * Agent tags to set node running on which agent
     */
    private Selector selector;

    /**
     * Node start trigger
     */
    private TriggerFilter trigger = new TriggerFilter();

    /**
     * Unix cron expression
     */
    private String cron;

    /**
     * Node before groovy script;
     */
    private String before;

    /**
     * Node execute script, can be null
     */
    private String script;

    /**
     * Plugin name
     */
    private String plugin;

    /**
     * Variables name to export to context
     */
    private Set<String> exports = new HashSet<>(0);

    /**
     * Is allow failure
     */
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    private boolean tail = IS_TAIL_DEFAULT;

    private Integer order = 0;

    private Node parent;

    private List<Node> children = new LinkedList<>();

    public Node(String name) {
        this.name = name;
        this.path = NodePath.create(name);
    }

    public String getPathAsString() {
        return path.getPathInStr();
    }

    public String getEnv(String name) {
        return environments.get(name);
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return !Strings.isNullOrEmpty(plugin);
    }

    @JsonIgnore
    public boolean hasBefore() {
        return !Strings.isNullOrEmpty(before);
    }

    @JsonIgnore
    public boolean hasExports() {
        return exports != null && !exports.isEmpty();
    }

    @JsonIgnore
    public boolean hasCron() {
        return !Strings.isNullOrEmpty(cron);
    }
}
