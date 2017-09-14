/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.api.domain.node;

import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author yang
 */
public class NodeTree {

    /**
     * The tree cache by path
     */
    private final Cache<String, Node> treeCache = CacheBuilder.newBuilder().build();

    /**
     * Ordered tree list without root, used for find next and prev steps
     */
    private final LinkedList<Node> children = new LinkedList<>();

    private final Node root;

    public NodeTree(Node root) {
        this.root = root;
        fill(this.root);
    }

    public NodeTree(String yml) {
        root = NodeUtil.buildFromYml(yml); // build from yml
        fill(this.root);
    }

    public List<Node> children() {
        return children;
    }

    public int childrenSize() {
        return children.size();
    }

    public Node root() {
        return root;
    }

    public Node find(String path) {
        return treeCache.getIfPresent(path);
    }

    /**
     * Find step next node for current path
     *
     * @return next node instance or null if not found
     */
    public Node next(String path) {
        Node current = find(path);
        return NodeUtil.next(current, children);
    }

    /**
     * Find prev step node for current path
     *
     * @return prev node instance or null if not found
     */
    public Node prev(String path) {
        Node current = find(path);
        return NodeUtil.prev(current, children);
    }

    /**
     * Get first node from order tree
     *
     * @return first node instance or null
     */
    public Node first() {
        try {
            return children.getFirst();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public Node last() {
        try {
            return children.getLast();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * node can run or not
     *
     * @return true or false
     */
    public Boolean canRun(String path) {
        Node node = find(path);
        return node.getChildren().size() == 0;
    }

    public void setEnv(String path, Map<String, String> envs) {
        Node node = find(path);
        if (node == null) {
            throw new IllegalParameterException("Node doesn't existed for path: " + path);
        }

        EnvUtil.merge(node.getEnvs(), envs, true);
    }

    public void delete(String path) {
        treeCache.invalidate(path);
    }

    public boolean exist(String path) {
        return find(path) != null;
    }

    private void fill(Node root) {
        NodeUtil.recurse(root, node -> {
            treeCache.put(node.getPath(), node);
            children.add(node);
        });
        children.remove(root);
    }
}
