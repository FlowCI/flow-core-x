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
        if (isRoot(current)) {
            return ordered.get(FirstOrder);
        }

        Node node = get(current);
        List<Node> nextNodes = ordered.get(node.getNextOrder());
        List<Node> list = new LinkedList<>();

        // next nodes parent should be current node
        if (node instanceof FlowNode) {
            for (Node next : nextNodes) {
                if ((next.parent.equals(node))) {
                    list.add(next);
                }
            }
            return list;
        }


        if (node instanceof RegularStepNode) {
            // handle current node is the last node of parent
            if (isLastNodeOfParent(node)) {
                list.addAll(nextNodes);
                return list;
            }

            // handle steps in step
            if (((RegularStepNode) node).hasChildren()) {
                for (Node next : nextNodes) {
                    if ((next.parent.equals(node))) {
                        list.add(next);
                    }
                }
                return list;
            }

            // next nodes should have same parent with current
            for (Node next : nextNodes) {
                if (next.parent.equals(node.getParent())) {
                    list.add(next);
                }
            }
            return list;
        }

        if (node instanceof ParallelStepNode) {
            list.addAll(nextNodes);
        }

        return list;
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
        if (parent == null) {
            return next(node.getPath()).get(0);
        }

        int nextOrder = node.getOrder() + 1;
        for (Node next : ordered.get(nextOrder)) {
            Node nextParent = next.getParent();
            if (nextParent != null && nextParent.equals(parent)) {
                return next;
            }

            return findNextWithSameParent(next, parent);
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
        });
    }

    private void buildGraph(FlowNode root) {
        maxOrder = buildGraph(root.getChildren(), 1) - 1;
    }

    /**
     * Root flow index is 0
     * Step index start from 1
     */
    private int buildGraph(List<Node> nodes, int order) {
        ordered.computeIfAbsent(order, integer -> new LinkedList<>());

        for (Node node : nodes) {
            node.setOrder(order);
            node.setNextOrder(order + 1);

            flatted.put(node.getPath(), node);
            ordered.get(order).add(node);

            if (node instanceof ParentNode) {
                RegularStepNode rStep = (RegularStepNode) node;
                order = buildGraph(rStep.getChildren(), order + 1);
            }

            if (node instanceof ParallelStepNode) {
                ParallelStepNode pStep = (ParallelStepNode) node;

                order++;
                ordered.computeIfAbsent(order, integer -> new LinkedList<>());

                for (Map.Entry<String, FlowNode> entry : pStep.getParallel().entrySet()) {
                    FlowNode flow = entry.getValue();
                    flow.setOrder(order);
                    flow.setNextOrder(order + 1);

                    flatted.put(flow.getPath(), flow);
                    ordered.get(order).add(flow);
                }


                int maxNextOrder = order;
                for (Map.Entry<String, FlowNode> entry : pStep.getParallel().entrySet()) {
                    FlowNode flow = entry.getValue();
                    int newOrder = buildGraph(flow.getChildren(), order + 1);
                    if (newOrder > maxNextOrder) {
                        maxNextOrder = newOrder;
                    }
                }

                // update next order of last node to maxNextOrder
                for (Map.Entry<String, FlowNode> entry : pStep.getParallel().entrySet()) {
                    FlowNode flow = entry.getValue();
                    int size = flow.getChildren().size();

                    Node lastNode = flow.getChildren().get(size - 1);
                    lastNode.setNextOrder(maxNextOrder);
                }

                order = maxNextOrder;
            }
        }

        return order;
    }
}
