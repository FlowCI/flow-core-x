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

import com.flowci.domain.LocalTask;
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

    /**
     * All condition list
     */
    @Getter
    private final Set<String> conditions = new HashSet<>(DEFAULT_SIZE);

    @Getter
    private final Set<String> plugins = new HashSet<>(DEFAULT_SIZE);

    public NodeTree(FlowNode root) {
        this.root = root;
        buildTree(root);
        buildMetaData(root, steps);
        buildCacheWithIndex();
    }

    public boolean isFirst(NodePath path) {
        StepNode node = steps.get(0);
        return node.getPath().equals(path);
    }

    /**
     * Find next Node instance from path
     */
    public StepNode next(NodePath path) {
        if (path.equals(root.getPath())) {
            return steps.get(0);
        }

        StepNode step = get(path);
        return findNext(step);
    }

    /**
     * Find step node instance that parent is flow
     */
    public StepNode nextRootStep(NodePath path) {
        if (path.equals(root.getPath())) {
            return steps.get(0);
        }

        StepNode next = findNext(get(path));
        if (Objects.isNull(next)) {
            return null;
        }

        if (next.getParent() instanceof FlowNode) {
            return next;
        }

        return nextRootStep(next.getPath());
    }

    /**
     * Get parent Node instance from path
     */
    public Nodeable parent(NodePath path) {
        return get(path).getParent();
    }

    public StepNode get(NodePath path) {
        StepNode step = cached.get(path);
        if (Objects.isNull(step)) {
            throw new IllegalArgumentException("The node path doesn't existed");
        }
        return step;
    }

    private StepNode findNext(StepNode current) {
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

    private void buildMetaData(FlowNode root, List<StepNode> steps) {
        if (root != null) {
            if (root.hasCondition()) {
                conditions.add(root.getCondition());
            }

            for (LocalTask t : root.getNotifications()) {
                if (t.hasPlugin()) {
                    plugins.add(t.getPlugin());
                }
            }
        }

        for (StepNode step : steps) {
            if (step instanceof RegularStepNode) {
                RegularStepNode rStep = (RegularStepNode) step;
                if (rStep.hasCondition()) {
                    conditions.add(rStep.getCondition());
                }

                if (rStep.hasPlugin()) {
                    plugins.add(rStep.getPlugin());
                }

                buildMetaData(null, rStep.getChildren());
                continue;
            }

            if (step instanceof ParallelStepNode) {
                ParallelStepNode pStep = (ParallelStepNode) step;
                pStep.getParallel().forEach((k, v) -> buildMetaData(v, v.getChildren()));
            }
        }
    }

    private void buildCacheWithIndex() {
        for (int i = 0; i < steps.size(); i++) {
            StepNode step = steps.get(i);
            step.setOrder(i);
            cached.put(step.getPath(), step);
        }
    }

    private void buildTree(ParentNode root) {
        for (StepNode step : root.getChildren()) {
            steps.add(step);

            if (step instanceof RegularStepNode) {
                buildTree((RegularStepNode) step);
            }
        }
    }
}
