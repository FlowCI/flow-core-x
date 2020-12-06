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

import com.flowci.exception.ArgumentException;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.*;

/**
 * @author yang
 */
public final class NodeTree {

    private static final int DefaultSize = 20;

    private static final int FirstOrder = 1;

    /**
     * Create node tree from FlowNode object
     */
    public static NodeTree create(FlowNode root) {
        return new NodeTree(root);
    }

    /**
     * Flatted step nodes
     */
    private final Map<NodePath, Node> flatted = new HashMap<>(DefaultSize);

    /**
     * Ordered graph
     */
    private final Map<Integer, List<Node>> ordered = new HashMap<>(DefaultSize);

    @Getter
    private final FlowNode root;

    @Getter
    private int maxOrder;

    /**
     * All condition list
     */
    @Getter
    private final Set<String> conditions = new HashSet<>(DefaultSize);

    @Getter
    private final Set<String> plugins = new HashSet<>(DefaultSize);

    public NodeTree(FlowNode root) {
        this.root = root;
        buildGraph(this.root);
        buildMetaData();
    }

    public int numOfNode() {
        return flatted.size();
    }

    /**
     * Find next Node instance from path
     */
    public List<Node> next(NodePath current) {
        return null;
    }

    private boolean isLastNodeOfParent(Node node) {
        if (!node.hasParent()) {
            return false;
        }

        ParentNode parent = (ParentNode) node.getParent();
        return parent.getLastNode().equals(node);
    }

    /**
     * Skip current node and return next nodes with the same root of current
     */
    public Node skip(NodePath current) {
        if (isRoot(current)) {
            return ordered.get(FirstOrder).get(0);
        }

        Node node = get(current);
        if (node == null) {
            return null;
        }

        Node parent = node.getParent();
        return findNextWithSameParent(node, parent);
    }

    /**
     * Return other parallel nodes that current node should waiting for
     */
    public List<Node> parallel(NodePath current) {
        return null;
    }

    /**
     * Get parent Node instance from path
     */
    public Node parent(NodePath path) {
        return get(path).getParent();
    }

    public Node get(NodePath path) {
        return flatted.get(path);
    }

    private boolean isRoot(NodePath path) {
        return path.equals(root.getPath());
    }

    private Node findNextWithSameParent(Node node, Node parent) {
        return null;
    }

    private void buildMetaData() {
        flatted.forEach((path, node) -> {
            if (node.hasCondition()) {
                conditions.add(node.getCondition());
            }

            if (node instanceof RegularStepNode) {
                RegularStepNode r = (RegularStepNode) node;

                if (r.hasPlugin()) {
                    plugins.add(r.getPlugin());
                }
            }
        });
    }

    private List<Node> buildGraph(Node root) {
        if (root instanceof ParentNode) {
            ParentNode n = (ParentNode) root;

            if (!n.hasChildren()) {
                return Lists.newArrayList(n);
            }

            List<Node> prevs = Lists.newArrayList(n);

            for (int i = 0; i < n.children.size(); i++) {
                Node current = n.children.get(i);
                for (Node prev : prevs) {
                    prev.next.add(current);
                }

                prevs = buildGraph(current);
            }

            return prevs;
        }

        if (root instanceof ParallelStepNode) {
            ParallelStepNode n = (ParallelStepNode) root;
            List<Node> prevs = new ArrayList<>(n.getParallel().size());

            n.getParallel().forEach((k, subflow) -> {
                n.next.add(subflow);
                prevs.addAll(buildGraph(subflow));
            });

            return prevs;
        }

        throw new ArgumentException("Un-support node type");
    }
}
