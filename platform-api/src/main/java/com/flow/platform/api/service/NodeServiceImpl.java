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
package com.flow.platform.api.service;

import com.flow.platform.api.domain.Node;
import com.flow.platform.api.util.NodeUtil;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {
    private final Map<String, Node> mocNodeList = new HashMap<>();

    @Override
    public Node create(Node node) {
        String path = UUID.randomUUID().toString();
        node.setPath(path);
        node.setCreatedAt(ZonedDateTime.now());
        node.setUpdatedAt(ZonedDateTime.now());
        mocNodeList.put(path, node);
        return node;
    }

    @Override
    public Boolean delete(Node node) {
        if(mocNodeList.remove(node.getPath()) == null){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public Node update(Node node) {
        return mocNodeList.put(node.getPath(), node);
    }

    @Override
    public Node find(String path) {
        return mocNodeList.get(path);
    }

    @Override
    public List<Node> listChildrenByNode(Node node) {
        List<Node> nodes = new LinkedList<>();
        for (Node item : mocNodeList.values()) {
            if(item.getParentId() == node.getPath()){
                nodes.add(item);
            }
        }

        return nodes;
    }

    @Override
    public Node prevNode(Node node) {
        return mocNodeList.get(node.getPrevId());
    }

    @Override
    public Node nextNode(Node node) {
        return mocNodeList.get(node.getNextId());
    }

    @Override
    public Node parent(Node node) {
        return mocNodeList.get(node.getParentId());
    }
}
