/*
 * Copyright 2018 flow.ci
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

package com.flowci.tree;

import com.flowci.exception.YmlException;
import com.flowci.tree.yml.FlowYml;
import com.flowci.util.YamlHelper;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import static com.flowci.tree.FlowNode.DEFAULT_ROOT_NAME;

/**
 * @author yang
 */
public class YmlParser {

    /**
     * Create Node instance from yml
     */
    public static synchronized FlowNode load(String yml) {
        Yaml yaml = YamlHelper.create(FlowYml.class);

        try {
            FlowYml root = yaml.load(yml);
            root.setName(DEFAULT_ROOT_NAME);
            return root.toNode();
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }
}
