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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * @author yang
 */
public class NodeTree {

    private final static int DEFAULT_SIZE = 20;

    /**
     * Create node tree from FlowNode object
     */
    public static NodeTree create(FlowNode root) {
        return new NodeTree(root);
    }

    private final Map<NodePath, StepNode> cached = new HashMap<>(DEFAULT_SIZE);

    @Getter
    private final List<StepNode> ordered = new ArrayList<>(DEFAULT_SIZE);

    @Getter
    private FlowNode root;

    public NodeTree(FlowNode root) {
        this.root = root;
        buildTree(root);

        moveFinalNodes();
        buildCacheWithIndex();
    }

    public boolean isFirst(NodePath path) {
        Node node = ordered.get(0);
        return node.getPath().equals(path);
    }

    public boolean isLast(NodePath path) {
        Node node = ordered.get(ordered.size() - 1);
        return node.getPath().equals(path);
    }

    /**
     * Get next Node instance from path
     */
    public StepNode next(NodePath path) {
        if (path.equals(root.getPath())) {
            return ordered.get(0);
        }

        StepNode step = get(path);
        int nextIndex = step.getOrder() + 1;
        if (nextIndex > (ordered.size() - 1)) {
            return null;
        }

        return ordered.get(nextIndex);
    }

    /**
     * Get next final node instance from path
     */
    public StepNode nextFinal(NodePath path) {
        if (path.equals(root.getPath())) {
            return nextFinal(ordered.get(0).getPath());
        }

        StepNode nodeWithIndex = get(path);
        if (nodeWithIndex.isTail()) {
            return next(path);
        }

        int nextIndex = nodeWithIndex.getOrder() + 1;
        if (nextIndex > (ordered.size() - 1)) {
            return null;
        }

        for (int i = nextIndex; i < ordered.size(); i++) {
            StepNode node = ordered.get(i);
            if (node.isTail()) {
                return node;
            }
        }

        return null;
    }

    /**
     * Get parent Node instance from path
     */
    public Node parent(NodePath path) {
        return get(path).getParent();
    }

    public StepNode get(NodePath path) {
        StepNode step = cached.get(path);

        if (Objects.isNull(step)) {
            throw new IllegalArgumentException("The node path doesn't existed");
        }

        return step;
    }

    /**
     * Move all final nodes to the tail
     */
    private void moveFinalNodes() {
        Iterator<StepNode> iterator = ordered.iterator();
        List<StepNode> finals = new LinkedList<>();

        while (iterator.hasNext()) {
            StepNode node = iterator.next();

            if (node.isTail()) {
                finals.add(node);
                iterator.remove();
            }
        }

        if (!finals.isEmpty()) {
            ordered.addAll(finals);
        }
    }

    private void buildCacheWithIndex() {
        for (int i = 0; i < ordered.size(); i++) {
            StepNode step = ordered.get(i);
            step.setOrder(i);
            cached.put(step.getPath(), step);
        }
    }

    /**
     * Reset node path and parent reference and put to cache
     */
    private void buildTree(Node root) {
        for (StepNode step : root.getChildren()) {
            step.setPath(NodePath.create(root.getPath(), step.getName()));
            step.setParent(root);

            buildTree(step);
        }

        if (root instanceof StepNode) {
            ordered.add((StepNode) root);
        }
    }
}
