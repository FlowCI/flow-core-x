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
import com.flow.platform.api.domain.Step;
import com.flow.platform.domain.Jsonable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
    NodeServiceImpl nodeService;

    public Node createNodeByYamlFile(String path) {
        try {
            String yamlString = preLoad(path);
            Yaml yaml = new Yaml();
            Map result = (Map) yaml.load(yamlString);
            return createNode(result);
        } catch (Exception e) {
            return null;
        }
    }

    public Node createNodeByYamlString(String yamlString) {
        Yaml yaml = new Yaml();
        Map result = (Map) yaml.load(yamlString);
        return createNode(result);
    }

    private Node createNode(Map result) {
        if (result.get("flow") != null) {
            String rawJson = Jsonable.GSON_CONFIG.toJson(result.get("flow"));
            Flow[] flows = Jsonable.GSON_CONFIG.fromJson(rawJson, Flow[].class);
            for (int i = 0; i < flows.length; i++) {
                flows[i].setPath("/" + flows[i].getName());
                if (i > 0) {
                    flows[i].setPrev(flows[i - 1]);
                    flows[i - 1].setNext(flows[i]);
                }
                if (flows[i].getChildren() != null) {
                    String stepJson = Jsonable.GSON_CONFIG.toJson(flows[i].getChildren());
                    flows[i].setChildren(new ArrayList<>());
                    Node node = recursion(stepJson, flows[i]);
                    return nodeService.create(node);
                }
            }
        }
        return null;
    }

    public String preLoad(String path) throws IOException {
        File yamlFile = new File(path);
        BufferedReader fileReader = new BufferedReader(new FileReader(yamlFile));
        String temp;
        StringBuilder stringBuilder = new StringBuilder();
        while ((temp = fileReader.readLine()) != null) {
            if (temp.trim().startsWith("!!")) {
                continue;
            }
            stringBuilder.append(temp + "\n");
        }
        String result = stringBuilder.toString();

        return result;
    }

    private Node recursion(String nodes, Node node) {
        Step[] steps = Jsonable.GSON_CONFIG.fromJson(nodes, Step[].class);
        for (int j = 0; j < steps.length; j++) {
            node.getChildren().add(steps[j]);
            steps[j].setParent(node);
            steps[j].setPath(node.getPath() + "/" + steps[j].getName());
            if (j > 0) {
                steps[j].setPrev(steps[j - 1]);
                steps[j - 1].setNext(steps[j]);
            }

            if (steps[j].getChildren() != null) {
                String stepJson1 = Jsonable.GSON_CONFIG.toJson(steps[j].getChildren());
                steps[j].setChildren(new ArrayList<>());
                recursion(stepJson1, steps[j]);
            }
        }
        return node;
    }
}
