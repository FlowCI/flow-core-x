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

import java.io.Serializable;
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
     * Create node tree from Node object
     */
    public static NodeTree create(Node root) {
        return new NodeTree(root);
    }

    private final Map<NodePath, NodeWithIndex> cached = new HashMap<>(DEFAULT_SIZE);

    @Getter
    private final List<Node> ordered = new ArrayList<>(DEFAULT_SIZE);

    @Getter
    private Node root;

    public NodeTree(Node root) {
        this.root = root;

        buildTree(root);
        ordered.remove(root);

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
     * Get previous Node instance from path
     */
    public Node prev(NodePath path) {
        NodeWithIndex nodeWithIndex = getWithIndex(path);

        if (nodeWithIndex.node.equals(root)) {
            return null;
        }

        int prevIndex = nodeWithIndex.index - 1;

        if (prevIndex < 0) {
            return null;
        }

        return ordered.get(prevIndex);
    }

    /**
     * Get next Node instance from path
     */
    public Node next(NodePath path) {
        NodeWithIndex nodeWithIndex = getWithIndex(path);

        int nextIndex = nodeWithIndex.index + 1;

        // next is out of range
        if (nextIndex > (ordered.size() - 1)) {
            return null;
        }

        return ordered.get(nextIndex);
    }

    /**
     * Get next final node instance from path
     */
    public Node nextFinal(NodePath path) {
        NodeWithIndex nodeWithIndex = getWithIndex(path);

        if (nodeWithIndex.node.isTail()) {
            return next(path);
        }

        int nextIndex = nodeWithIndex.index + 1;
        if (nextIndex > (ordered.size() - 1)) {
            return null;
        }

        for (int i = nextIndex; i < ordered.size(); i++) {
            Node node = ordered.get(i);
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
        return getWithIndex(path).node.getParent();
    }

    public Node get(NodePath path) {
        return getWithIndex(path).node;
    }

    public String toYml() {
        return YmlParser.parse(this.root);
    }

    private NodeWithIndex getWithIndex(NodePath path) {
        NodeWithIndex nodeWithIndex = cached.get(path);

        if (Objects.isNull(nodeWithIndex)) {
            throw new IllegalArgumentException("The node path doesn't existed");
        }

        return nodeWithIndex;
    }

    /**
     * Move all final nodes to the tail
     */
    private void moveFinalNodes() {
        Iterator<Node> iterator = ordered.iterator();

        List<Node> finals = new LinkedList<>();

        while (iterator.hasNext()) {
            Node node = iterator.next();
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
            Node node = ordered.get(i);
            cached.put(node.getPath(), new NodeWithIndex(node, i));
        }

        // set root index to -1
        cached.put(root.getPath(), new NodeWithIndex(root, -1));
    }

    /**
     * Reset node path and parent reference and put to cache
     */
    private void buildTree(Node root) {
        for (Node child : root.getChildren()) {
            child.setPath(NodePath.create(root.getPath(), child.getName()));
            child.setParent(root);
            buildTree(child);
        }

        ordered.add(root);
    }

    private class NodeWithIndex implements Serializable {

        Node node;

        int index;

        NodeWithIndex(Node node, int index) {
            this.node = node;
            this.index = index;
        }
    }
}
