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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

/**
 * @author yang
 */
public class YmlParser {

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    private final static Map<String, Integer> FieldsOrder = ImmutableMap.<String, Integer>builder()
        .put("envs", 1)
        .put("trigger", 2)
        .put("selector", 3)
        .put("cron", 4)
        .put("notifications", 5)
        .put("steps", 6)
        .build();

    /**
     * Create Node instance from yml
     */
    public static synchronized FlowNode load(String defaultName, String yml) {
        Yaml yaml = YamlHelper.create(FlowYml.class);

        try {
            FlowYml root = yaml.load(yml);

            // set default flow name if not defined in yml
            if (Strings.isNullOrEmpty(root.getName())) {
                root.setName(defaultName);
            }

            return root.toNode();
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }

    public static synchronized String parse(FlowNode root) {
        FlowYml flow = new FlowYml(root);
        Yaml yaml = YamlHelper.create(FieldsOrder, FlowYml.class);
        String dump = yaml.dump(flow);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }
}
