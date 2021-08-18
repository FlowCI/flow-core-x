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
import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.*;

/**
 * @author yang
 */
@Getter
public final class NodeTree {

    private static final int DefaultSize = 10;

    private static final int DefaultSizeForPrev = 5;

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

    private final Set<String> secrets = new HashSet<>(DefaultSize);

    private final Set<String> configs = new HashSet<>(DefaultSize);

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

    public Collection<Node> prevs(Collection<Node> nodes, boolean post) {
        Collection<Node> ps = prevs(nodes);

        if (!post) {
            return ps;
        }

        Set<Node> prevPost = new HashSet<>(DefaultSizeForPrev);
        for (Node p : ps) {
            if (isPostStep(p)) {
                prevPost.add(p);
            }
        }

        if (prevPost.isEmpty()) {
            return prevs(ps, true);
        }

        return prevPost;
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

    /**
     * Find next post step
     */
    public List<Node> post(NodePath path) {
        Node n = get(path);

        // check if step in parallel
        if (!isPostStep(n)) {
            ParallelStepNode parent = n.getParent(ParallelStepNode.class);
            if (parent != null) {
                return Lists.newArrayList(findPostSteps(parent));
            }
        }

        Collection<Node> post = new HashSet<>();
        for (Node next : n.next) {
            post.addAll(findNextPost(next));
        }

        return Lists.newArrayList(post);
    }

    public List<Node> post(String path) {
        return post(NodePath.create(path));
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

    /**
     * Get all previous node list from path
     */
    private Collection<Node> prevs(Collection<Node> nodes) {
        Set<Node> list = new HashSet<>(DefaultSizeForPrev);
        for (Node n : nodes) {
            list.addAll(n.getPrev());
        }
        return list;
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

                if (r.hasSecrets()) {
                    secrets.addAll(r.getSecrets());
                }

                if (r.hasConfigs()) {
                    configs.addAll(r.getConfigs());
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

    private Collection<Node> findNextPost(Node node) {
        if (node instanceof ParallelStepNode) {
            Collection<Node> output = findPostSteps((ParallelStepNode) node);
            if (!output.isEmpty()) {
                return output;
            }
        }

        if (node instanceof FlowNode) {
            Collection<Node> output = findPostSteps((FlowNode) node);
            if (!output.isEmpty()) {
                return output;
            }
        }

        if (node instanceof RegularStepNode) {
            Collection<Node> output = findPostSteps((RegularStepNode) node);
            if (!output.isEmpty()) {
                return output;
            }
        }

        Collection<Node> post = new HashSet<>();
        for (Node next : node.next) {
            post.addAll(findNextPost(next));
        }
        return post;
    }

    private Collection<Node> findPostSteps(ParallelStepNode p) {
        Collection<Node> post = new HashSet<>();
        for (Node child : p.getChildren()) {
            FlowNode f = (FlowNode) child;
            post.addAll(findPostSteps(f));
        }
        return post;
    }

    private Collection<Node> findPostSteps(FlowNode f) {
        Collection<Node> post = new HashSet<>();
        for (Node child : f.getChildren()) {
            if (child instanceof RegularStepNode) {
                post.addAll(findPostSteps((RegularStepNode) child));
            }

            if (child instanceof ParallelStepNode) {
                post.addAll(findPostSteps((ParallelStepNode) child));
            }
        }
        return post;
    }

    private Collection<Node> findPostSteps(RegularStepNode r) {
        if (r.isPost()) {
            return Sets.newHashSet(r);
        }
        return Collections.emptyList();
    }

    private static boolean isPostStep(Node n) {
        if (n instanceof RegularStepNode) {
            return ((RegularStepNode) n).isPost();
        }
        return false;
    }
}
