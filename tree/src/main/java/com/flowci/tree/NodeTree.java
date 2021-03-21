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
@Getter
public final class NodeTree {

    private static final int DefaultSize = 20;

    /**
     * Create node tree from FlowNode object
     */
    public static NodeTree create(FlowNode root) {
        return new NodeTree(root);
    }

    /**
     * Flatted nodes
     */
    private final Map<NodePath, Node> flatted = new HashMap<>(DefaultSize);

    private final FlowNode root;

    private final Set<Node> ends = new HashSet<>();

    private final Set<Selector> selectors = new HashSet<>();

    private final Set<String> conditions = new HashSet<>(DefaultSize);

    private final Set<String> plugins = new HashSet<>(DefaultSize);

    private int maxHeight = 1;

    public NodeTree(FlowNode root) {
        this.root = root;
        buildGraph(this.root);
        buildMetaData();
        buildEndNodes();
    }

    public int numOfNode() {
        return flatted.size();
    }

    /**
     * Get all last steps
     *
     * @return
     */
    public Collection<Node> ends() {
        return ends;
    }

    /**
     * Get all previous node list from path
     */
    public Collection<Node> prevs(List<Node> nodes) {
        List<Node> list = new LinkedList<>();
        for (Node n : nodes) {
            list.addAll(n.getPrev());
        }
        return list;
    }

    /**
     * Skip current node and return next nodes with the same root of current
     */
    public List<Node> skip(NodePath current) {
        Node node = get(current);

        Node parent = node.getParent();
        if (parent == null) {
            return Collections.emptyList();
        }

        if (parent instanceof ParallelStepNode) {
            parent = parent.parent;
        } else {
            List<Node> children = parent.getChildren();
            if (children.get(children.size() - 1).equals(node)) {
                return node.next;
            }
        }

        Node nextWithSameParent = findNextWithSameParent(node, parent);
        if (nextWithSameParent == null) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(nextWithSameParent);
    }

    public Node get(NodePath path) {
        Node node = flatted.get(path);
        if (node == null) {
            throw new ArgumentException("invalid node path {0}", path.getPathInStr());
        }
        return node;
    }

    public Node get(String nodePath) {
        return get(NodePath.create(nodePath));
    }

    private Node findNextWithSameParent(Node node, Node parent) {
        for (Node next : node.next) {
            if (parent.equals((next.parent))) {
                return next;
            }

            Node n = findNextWithSameParent(next, parent);
            if (n != null) {
                return n;
            }
        }

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

            if (node instanceof FlowNode) {
                FlowNode f = (FlowNode) node;
                selectors.add(f.getSelector());
            }
        });
    }

    /**
     * Build graph from yaml tree
     */
    private List<Node> buildGraph(Node root) {
        flatted.put(root.getPath(), root);

        if (!root.hasChildren()) {
            return Lists.newArrayList(root);
        }

        if (root instanceof ParallelStepNode) {
            ParallelStepNode n = (ParallelStepNode) root;

            int height = n.getParallel().size();
            List<Node> prevs = new ArrayList<>(height);

            if (maxHeight < height) {
                maxHeight = height;
            }

            n.getParallel().forEach((k, subflow) -> {
                subflow.prev.add(n);
                n.next.add(subflow);

                prevs.addAll(buildGraph(subflow));
            });

            return prevs;
        }

        List<Node> prevs = Lists.newArrayList(root);

        for (int i = 0; i < root.getChildren().size(); i++) {
            Node current = root.getChildren().get(i);
            current.prev.addAll(prevs);

            for (Node prev : prevs) {
                prev.next.add(current);
            }

            prevs = buildGraph(current);
        }

        return prevs;
    }

    private void buildEndNodes() {
        flatted.forEach((k, v) -> {
            if (v.next.isEmpty()) {
                ends.add(v);
            }
        });
    }

    private static boolean isPostStep(Node n) {
        if (n instanceof RegularStepNode) {
            RegularStepNode r = (RegularStepNode) n;
            return r.isPost();
        }
        return false;
    }
}
