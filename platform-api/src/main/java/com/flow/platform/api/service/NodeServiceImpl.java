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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.exception.IllegalParameterException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final Map<String, Node> mocNodeList = new HashMap<>();

    @Autowired
    private YmlStorageService ymlStorageService;

    @Autowired
    private FlowDao flowDao;

    @Override
    public Node create(Node node) {
        NodeUtil.recurse(node, item -> {
            String env = System.getProperty("flow.api.env");
            if (item instanceof Step && env != "test") {
                if (NodeUtil.canRun(item) && (item.getScript() == null || item.getScript().isEmpty())) {
                    throw new IllegalParameterException(
                        String.format("Missing Param Script, NodeName: %s", item.getName()));
                }
            }

            save(item);
        });
        return node;
    }

    @Override
    public Node save(Node node) {
        mocNodeList.put(node.getPath(), node);

        if(node instanceof  Flow){
            Flow flow = flowDao.get(node.getPath());
            if(flow == null){
                flowDao.save((Flow) node);
            }
        }

        return node;
    }



    @Override
    public Node find(String nodePath) {
        Node node = mocNodeList.get(nodePath);
        if (node == null) {
            create(NodeUtil.buildFromYml(ymlStorageService.get(nodePath).getFile()));
            return find(nodePath);
        }else{
            return node;
        }
    }
}
