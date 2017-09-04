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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * @author yh@firim
 */
public class YmlParser {

    public static <T> T fromObject(Object o, Type typeOfT) {
        return (T) TypeAdaptorFactory.getAdaptor(typeOfT).read(o);
    }

    public static <T> Object toObject(T t) {
        return TypeAdaptorFactory.getAdaptor(t.getClass()).write(t);
    }

    public static <T> T fromYml(String str, Type typeOfT) {
        Yaml yaml = new Yaml();
        Map result = (Map) yaml.load(str);
        return fromObject(result.get("flow"), typeOfT);
    }

    public static <T> String toYml(T t) {
        Map<String, Object> map = new HashMap<>();
        Object o = toObject(t);
        map.put("flow", o);
        Yaml yaml = new Yaml();
        return yaml.dump(map);
    }
}
