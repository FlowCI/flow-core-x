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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.Node;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Component;
import sun.rmi.server.InactiveGroupException;

/**
 * @author yh@firim
 */

@Component(value = "nodeUtil")
public class NodeUtil {

    /**
     * get all children elements from node
     */
    public List<Node> allChildren(Node node) {

        List<Node> allNodes = new LinkedList<>();
        List<Node> nodes = childrenOrdered(node);

        nodes.forEach(item -> {
            List<Node> nodeList = allChildren(item);
            nodeList.forEach(ite -> {
                allNodes.add(ite);
            });
        });

        //not flow node
        if (detectFlowNode(node) == false) {
            allNodes.add(node);
        }

        return allNodes;
    }

    /**
     * detect is flow or jobFlow
     */
    private Boolean detectFlowNode(Node node) {
        if ((node instanceof Flow) || (node instanceof JobFlow)) {
            return true;
        }
        return false;
    }


    /**
     * ordered children element
     */
    public List<Node> childrenOrdered(Node node) {
        List<Node> nodeList = new LinkedList<>();
        final Node[] first = new Node[1];

        node.getChildren().forEach((Node item) -> {
            if (item.getPrev() == null) {
                first[0] = item;
            }
        });

        if (first.length != 0) {
            Node firstNode = first[0];
            if (firstNode != null) {
                nodeList.add(firstNode);
                while (true) {
                    firstNode = firstNode.getNext();
                    if (firstNode == null) {
                        break;
                    }
                    nodeList.add(firstNode);
                }
            }
        }

        if (node.getChildren().size() != nodeList.size()) {
            throw new RuntimeException("this node illegal");
        }
        return nodeList;
    }


    /**
     * detect children element legal
     */
    public Boolean detectChildrenNodeLegal(Node node) {
        return node.getChildren().size() == childrenOrdered(node).size();
    }

    /**
     * get pre node from all children element
     */
    public Node prevNodeFromAllChildren(Node node) {
        Node parentFlowNode = parentFlowNode(node);
        List<Node> nodes = allChildren(parentFlowNode);
        Integer flag = 0;
        Node targetNode = null;
        for (Node item : nodes) {
            if (item == node) {
                targetNode = item;
                break;
            }
            flag = flag + 1;
        }
        if (targetNode != null && flag >= 1) {
            return nodes.get(flag - 1);
        }
        return null;
    }

    /**
     * get parent flow
     */
    private Node parentFlowNode(Node node) {
        if (detectFlowNode(node)) {
            return node;
        }
        return parentFlowNode(node.getParent());
    }

    /**
     * get next node from all children element
     */
    public Node nextNodeFromAllChildren(Node node) {
        Node parentFlowNode = parentFlowNode(node);
        List<Node> nodes = allChildren(parentFlowNode);
        Integer flag = 0;
        Node targetNode = null;
        for (Node item : nodes) {
            if (item == node) {
                targetNode = item;
                break;
            }
            flag = flag + 1;
        }
        if (targetNode != null && flag <= nodes.size() - 2) {
            return nodes.get(flag + 1);
        }
        return null;
    }
}
