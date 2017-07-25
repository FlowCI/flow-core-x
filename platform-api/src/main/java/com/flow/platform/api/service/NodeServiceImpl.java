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
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final Map<String, Node> mocNodeList = new HashMap<>();

    @Override
    public Node create(Node node) {
        NodeUtil.recurse(node, item -> {
            if(item.getScript() == null || item.getScript().isEmpty()){
                throw new RuntimeException("create node error script is error");
            }
            save(item);
        });
        return node;
    }

    @Override
    public Node save(Node node) {
        mocNodeList.put(node.getPath(), node);
        return node;
    }

    @Override
    public Node find(String nodePath) {
        return mocNodeList.get(nodePath);
    }
}
