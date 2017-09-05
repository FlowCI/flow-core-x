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

package com.flow.platform.yml.parser;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlConfig.WriteClassName;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.flow.platform.yml.parser.exception.YmlParseException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * @author yh@firim
 */
public class YmlParser {

    private final static YamlConfig yamlConfig = new YamlConfig();

    static {
        yamlConfig.writeConfig.setAutoAnchor(false);
        yamlConfig.writeConfig.setWriteClassname(WriteClassName.NEVER);
    }

    private static <T> T fromObject(Object o, Type typeOfT) {
        if (o == null) {
            throw new YmlParseException("yml parser error");
        }
        return (T) TypeAdaptorFactory.getAdaptor(typeOfT).read(o);
    }

    private static <T> Object toObject(T t) {
        return TypeAdaptorFactory.getAdaptor(t.getClass()).write(t);
    }

    /**
     * Yml String To Type
     *
     * @param str Yml String
     * @param typeOfT Class
     * @return T instance
     */
    public static <T> T fromYml(String str, Type typeOfT) {
        Yaml yaml = new Yaml();
        Map result;
        try {
            result = (Map) yaml.load(str);
        } catch (Throwable throwable) {
            throw new YmlParseException("Load Yml error");
        }
        return fromObject(result.get("flow"), typeOfT);
    }

    /**
     * T to Yml
     *
     * @param t object
     * @return String
     */
    public static <T> String toYml(T t) {
        Map<String, Object> map = new HashMap<>();
        Object o = toObject(t);
        map.put("flow", o);
        String yml = null;

        try {
            Writer stringWriter = new StringWriter();
            YamlWriter writer = new YamlWriter(stringWriter, yamlConfig);
            writer.write(map);
            writer.close();
            yml = stringWriter.toString();
        } catch (Throwable throwable) {
            throw new YmlParseException(String.format("Object to yaml error"), throwable);
        }

        return yml;
    }
}
