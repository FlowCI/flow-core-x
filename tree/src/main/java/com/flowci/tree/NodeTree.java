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

import lombok.Getter;

import java.util.*;

/**
 * @author yang
 */
public class NodeTree {

    private static final int DEFAULT_SIZE = 20;

    /**
     * Create node tree from FlowNode object
     */
    public static NodeTree create(FlowNode root) {
        return new NodeTree(root);
    }

    private final Map<NodePath, StepNode> cached = new HashMap<>(DEFAULT_SIZE);

    @Getter
    private final List<StepNode> steps = new ArrayList<>(DEFAULT_SIZE);

    @Getter
    private final FlowNode root;

    public NodeTree(FlowNode root) {
        this.root = root;
        buildTree(root);
        buildCacheWithIndex();
    }

    public boolean isFirst(NodePath path) {
        Node node = steps.get(0);
        return node.getPath().equals(path);
    }

    /**
     * Get next Node instance from path
     */
    public StepNode next(NodePath path) {
        if (path.equals(root.getPath())) {
            return steps.get(0);
        }

        StepNode step = get(path);
        return findNext(step, steps);
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

    private StepNode findNext(StepNode current, List<StepNode> steps) {
        if (steps.isEmpty()) {
            return null;
        }

        if (Objects.isNull(current)) {
            return steps.get(0);
        }

        int next = current.getOrder() + 1;
        if (next > steps.size() - 1) {
            return null;
        }
        return steps.get(next);
    }

    private void buildCacheWithIndex() {
        for (int i = 0; i < steps.size(); i++) {
            StepNode step = steps.get(i);
            step.setOrder(i);
            cached.put(step.getPath(), step);
        }
    }

    private void buildTree(Node root) {
        for (StepNode step : root.getChildren()) {
            steps.add(step);
            buildTree(step);
        }
    }
}
