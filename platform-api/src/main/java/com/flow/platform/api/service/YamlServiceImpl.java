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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.domain.Jsonable;
import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;


/**
 * @author lhl
 */
@Service(value = "yamlService")
public class YamlServiceImpl implements YamlService {

    @Autowired
    private NodeServiceImpl nodeService;

    @Override
    public Node createNode(File path) {
        try {
            String yamlString = Files.toString(path, Charset.forName("UTF-8"));
            Yaml yaml = new Yaml();
            Map result = (Map) yaml.load(yamlString);
            return createNode(result);
        } catch (Throwable ignore) {
            return null;
        }
    }

    @Override
    public Node createNode(String yamlString) {
        Yaml yaml = new Yaml();
        Map result = (Map) yaml.load(yamlString);
        return createNode(result);
    }

    private Node createNode(Map result) {
        if (result.get("flow") != null) {
            String rawJson = Jsonable.GSON_CONFIG.toJson(result.get("flow"));
            Flow[] flows = Jsonable.GSON_CONFIG.fromJson(rawJson, Flow[].class);

            // build flow path and relation
            for (int i = 0; i < flows.length; i++) {
                flows[i].setPath("/" + flows[i].getName());
                if (i > 0) {
                    flows[i].setPrev(flows[i - 1]);
                    flows[i - 1].setNext(flows[i]);
                }
                buildNodeRelation(flows[i]);
            }

            return flows[0];
        }

        return null;
    }

    private <T extends Node> void buildNodeRelation(Node<T> root) {
        List<T> children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            T childNode = children.get(i);
            childNode.setPath(String.format("%s/%s", root.getPath(), childNode.getName()));
            childNode.setParent(root);
            if (i > 0) {
                childNode.setPrev(children.get(i - 1));
                children.get(i - 1).setNext(childNode);
            }
            buildNodeRelation(childNode);
        }
    }
}
