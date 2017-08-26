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
import java.util.Map;

/**
 * @author yang
 */
public class NodeTree {

    private final Cache<String, Node> treeCache = CacheBuilder.newBuilder().build();

    private final Node root;

    public NodeTree(Node root) {
        this.root = root;
        NodeUtil.recurse(root, node -> treeCache.put(node.getPath(), node));
    }

    public NodeTree(String yml) {
        root = NodeUtil.buildFromYml(yml); // build from yml
        NodeUtil.recurse(root, node -> treeCache.put(node.getPath(), node)); // put node to cache
    }

    public Node root() {
        return root;
    }

    public Node find(String path) {
        return treeCache.getIfPresent(path);
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
}
