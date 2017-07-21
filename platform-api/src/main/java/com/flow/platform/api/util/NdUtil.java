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


public class NdUtil {

    public static List<Node> deptNodes(Node node){
        List<Node> nodes = new LinkedList<>();
        List<Node> children = node.getChildren();
        for (Node child : children) {
            List<Node> nodeList = deptNodes(child);
            nodes.addAll(nodeList);
        }
        nodes.add(node);
        return nodes;
    }
}
