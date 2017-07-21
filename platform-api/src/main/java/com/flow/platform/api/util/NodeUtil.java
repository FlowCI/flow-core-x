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
package com.flow.platform.api.util;

import com.flow.platform.api.domain.Node;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yh@firim
 */
public class NodeUtil {

    public static void recurse(final Node root, final Consumer<Node> onNode) {
        for (Node child : root.getChildren()) {
            recurse(child, onNode);
        }
        onNode.accept(root);
    }


    public static Node findRootNode(Node node) {
        if (node.getParent() == null) {
            return node;
        }

        return findRootNode(node.getParent());
    }

    public static List<Node> flat(final Node node) {
        final List<Node> flatted = new LinkedList<>();
        recurse(node, flatted::add);
        return flatted;
    }

    /**
     * get prev node from flow
     * @param node
     * @return
     */
    public static Node prev(Node node) {
        Node root = findRootNode(node);
        List<Node> nodes = flat(root);
        Integer index = -1;
        Node target = null;
        for (int i = 0; i < nodes.size(); i++) {
            Node item = nodes.get(i);
            if(item.getPath() == node.getPath()){
                index = i;
            }
        }
        if(index >= 1 && index != -1 && index < nodes.size() - 1){
            target = nodes.get(index - 1);
        }

        return target;
    }

    /**
     * get next node from flow
     * @param node
     * @return
     */
    public static Node next(Node node) {
        Node root = findRootNode(node);
        List<Node> nodes = flat(root);
        Integer index = -1;
        Node target = null;
        for (int i = 0; i < nodes.size(); i++) {
            Node item = nodes.get(i);
            if(item.getPath() == node.getPath()){
                index = i;
            }
        }
        if(index <= nodes.size() - 3 && index != -1){
            target = nodes.get(index + 1);
        }

        if(index == nodes.size() - 1 && nodes.size() != 1){
            target = nodes.get(0);
        }
        return target;
    }
}
