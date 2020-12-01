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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.Serializable;
import java.util.*;

/**
 * @author yang
 */
@ToString(of = {"pathInStr"})
@EqualsAndHashCode(of = {"pathInStr"})
public final class NodePath implements Serializable {

    public static final String PathSeparator = "/";

    private static final Set<String> Reserved = Sets.newHashSet("*", ";", ".", "/");

    private static final int MaxDepth = 10;

    private static final Range<Integer> NameLengthRange = Range.closed(1, 100);

    private final List<String> paths = new ArrayList<>(MaxDepth);

    @Getter
    private final String pathInStr;

    public static NodePath create(String... nameOrPaths) {

        return new NodePath(Arrays.asList(nameOrPaths));
    }

    public static NodePath create(NodePath parentPath, String... nameOrPaths) {
        List<String> paths = new LinkedList<>();
        if (!Objects.isNull(parentPath)) {
            paths.addAll(parentPath.paths);
        }
        paths.addAll(Arrays.asList(nameOrPaths));
        return new NodePath(paths);
    }

    private NodePath(List<String> nameOrPaths) {
        for (String nameOrPath : nameOrPaths) {
            if (nameOrPath == null) {
                continue;
            }

            nameOrPath = nameOrPath.trim();

            if (nameOrPath.startsWith(PathSeparator)) {
                nameOrPath = nameOrPath.substring(1);
            }

            // name include path separator
            String[] names = nameOrPath.split(PathSeparator);
            if (names.length > 0) {
                for (String name : names) {
                    if (Strings.isNullOrEmpty(name.trim())) {
                        continue;
                    }

                    if (!validate(name)) {
                        throw new YAMLException("Illegal node name: " + name);
                    }

                    paths.add(name);
                }
                continue;
            }

            String name = nameOrPath;
            if (Strings.isNullOrEmpty(name)) {
                continue;
            }

            if (!validate(name)) {
                throw new YAMLException("Illegal node name: " + name);
            }

            paths.add(name);
        }

        if (paths.isEmpty()) {
            throw new YAMLException("Empty node path is not allowed");
        }

        if (paths.size() > MaxDepth) {
            throw new YAMLException("Node path over the depth limit");
        }

        StringBuilder builder = new StringBuilder();
        for (String name : paths) {
            builder.append(name).append(PathSeparator);
        }
        pathInStr = builder.deleteCharAt(builder.length() - 1).toString();
    }

    public String getNodePathWithoutSpace() {
        return pathInStr.replace(" ", "-");
    }

    public boolean isRoot() {
        return paths.size() == 1;
    }

    public NodePath parent() {
        if ((paths.size() - 1) >= 0) {
            paths.remove(paths.size() - 1);
            return new NodePath(paths);
        }
        return null;
    }

    public NodePath root() {
        return new NodePath(Lists.newArrayList(paths.get(0)));
    }

    public String name() {
        return paths.get(paths.size() - 1);
    }

    /**
     * Validate node name with the criteria
     * - not empty
     * - cannot start with '/'
     * - 1 <= length <= 100
     * - cannot contains '*', '.', '/', ';'
     */
    public static boolean validate(String name) {
        name = name.trim();

        if (Strings.isNullOrEmpty(name) || name.startsWith(PathSeparator)) {
            return false;
        }

        if (!NameLengthRange.contains(name.length())) {
            return false;
        }

        for (String keyword : Reserved) {
            if (name.contains(keyword)) {
                return false;
            }
        }

        return true;
    }
}
