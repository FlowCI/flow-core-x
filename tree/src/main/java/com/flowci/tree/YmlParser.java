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

import com.flowci.common.exception.YmlException;
import com.flowci.common.helper.YamlHelper;
import com.flowci.tree.yml.FlowYml;
import org.yaml.snakeyaml.error.YAMLException;

import static com.flowci.tree.FlowNode.DEFAULT_ROOT_NAME;

/**
 * @author yang
 */
public class YmlParser {

    /**
     * Create Node instance from yml
     */
    public static synchronized FlowNode load(String ...ymls) {
        if (ymls.length == 0) {
            return new FlowNode(DEFAULT_ROOT_NAME);
        }

        try {
            var root = new FlowYml();
            for (var yml : ymls) {
                var ymlObj = YamlHelper.create(FlowYml.class);
                FlowYml tmp = ymlObj.load(yml);
                root.merge(tmp);
            }
            root.setName(DEFAULT_ROOT_NAME);
            return root.toNode(null);
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }
}
