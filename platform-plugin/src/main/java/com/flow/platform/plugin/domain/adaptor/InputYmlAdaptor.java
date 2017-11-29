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

package com.flow.platform.plugin.domain.adaptor;

import com.flow.platform.plugin.domain.envs.BooleanPluginEnvKey;
import com.flow.platform.plugin.domain.envs.IntegerPluginEnvKey;
import com.flow.platform.plugin.domain.envs.ListPluginEnvKey;
import com.flow.platform.plugin.domain.envs.PluginEnvKey;
import com.flow.platform.plugin.domain.envs.StringPluginEnvKey;
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.jgit.util.BlockList;

/**
 * @author yh@firim
 */
public class InputYmlAdaptor extends YmlAdaptor<List<PluginEnvKey>> {

    private final static Collection<Factory> factories = ImmutableSet.of(
        new StringFactory(),
        new IntegerFactory(),
        new BooleanFactory(),
        new ListFactory()
    );

    private static PluginEnvKey handle(Map map) {
        PluginEnvKey pluginEnvKey = null;
        for (Factory adaptor : factories) {
            pluginEnvKey = adaptor.handle(map);
            if (!Objects.isNull(pluginEnvKey)) {
                break;
            }
        }
        return pluginEnvKey;
    }

    @Override
    public List<PluginEnvKey> read(Object o) {
        List<PluginEnvKey> list = new BlockList<>();

        for (Map map : (ArrayList<Map>) o) {
            PluginEnvKey pluginEnvKey = handle(map);
            if (!Objects.isNull(pluginEnvKey)) {
                list.add(pluginEnvKey);
            }
        }

        return list;
    }

    @Override
    public Object write(List<PluginEnvKey> pluginEnvKeys) {
        return null;
    }

    private interface Factory {

        PluginEnvKey handle(Map map);
    }

    private static class StringFactory implements Factory {

        @Override
        public PluginEnvKey handle(Map map) {
            if (Objects.equals("string", map.get("type"))) {
                return new StringPluginEnvKey(
                    getMapValue(map, "name"),
                    getMapValue(map, "default"),
                    booleanOfString(getMapValue(map, "required"))
                );
            }

            return null;
        }
    }

    private static class IntegerFactory implements Factory {

        @Override
        public PluginEnvKey handle(Map map) {
            if (Objects.equals("integer", map.get("type"))) {
                return new IntegerPluginEnvKey(
                    getMapValue(map, "name"),
                    integerOfString(getMapValue(map, "default")),
                    booleanOfString(getMapValue(map, "required"))
                );
            }

            return null;
        }
    }

    private static class BooleanFactory implements Factory {

        @Override
        public PluginEnvKey handle(Map map) {
            if (Objects.equals("boolean", map.get("type"))) {
                return new BooleanPluginEnvKey(
                    getMapValue(map, "name"),
                    booleanOfString(getMapValue(map, "default")),
                    booleanOfString(getMapValue(map, "required"))
                );
            }

            return null;
        }
    }

    private static class ListFactory implements Factory {

        @Override
        public PluginEnvKey handle(Map map) {
            if (Objects.equals("list", map.get("type"))) {
                PluginEnvKey pluginEnvKey = new ListPluginEnvKey(
                    getMapValue(map, "name"),
                    getMapValue(map, "default"),
                    booleanOfString(getMapValue(map, "required"))
                );

                pluginEnvKey.setValues((List<String>) map.get("values"));
                return pluginEnvKey;
            }

            return null;
        }
    }


    private static String getMapValue(Map map, String name) {
        Object object = map.get(name);
        if (Objects.isNull(object)) {
            return null;
        }

        return object.toString();
    }

    private static boolean booleanOfString(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        } else {
            return Boolean.valueOf(value);
        }
    }


    private static Integer integerOfString(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        } else {
            return Integer.valueOf(value);
        }
    }

}
