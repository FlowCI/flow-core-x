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
package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.Node;
import com.flow.platform.api.service.YamlService;
import com.flow.platform.api.test.TestBase;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class YamlServiceTest extends TestBase {
    @Autowired
    YamlService yamlService;

    @Test
    public void should_create_node_by_file(){
        String path = "/Users/fir/Projects/Flow/flow-platform/flow.yaml";
        Node node = yamlService.createNodeByYamlFile(path);
        Assert.assertEquals("flow1", node.getName());

    }

    @Test
    public void should_create_node_by_string(){
        String path = "/Users/fir/Projects/Flow/flow-platform/flow.yaml";
        try {
            String yamlString = yamlService.preLoad(path);
            Node node = yamlService.createNodeByYamlString(yamlString);
            Assert.assertEquals("flow1", node.getName());
        } catch (IOException e){

        }
    }

    @Test
    public void parse_yaml_by_file(){
        String path = "/Users/fir/Projects/Flow/flow-platform/flow.yaml";
        try {
            String yamlString = yamlService.preLoad(path);
            Assert.assertEquals(true, yamlString.contains("step1"));
        } catch (IOException e){

        }
    }

}
