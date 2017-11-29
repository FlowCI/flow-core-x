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

package com.flow.platform.plugin.util;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.flow.platform.yml.parser.YmlParser;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author yh@firim
 */
public class YmlUtil {

    private final static YamlConfig yamlConfig = new YamlConfig();

    public static <T> T fromYml(String yml, Type type) {
        Map result = null;
        try {
            YamlReader yamlReader = new YamlReader(yml, yamlConfig);
            result = (Map) yamlReader.read();
        } catch (Throwable throwable) {
        }
        return YmlParser.fromObject(result, type);
    }
}
