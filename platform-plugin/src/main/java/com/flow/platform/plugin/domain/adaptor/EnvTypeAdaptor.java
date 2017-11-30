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

import com.flow.platform.plugin.domain.envs.PluginEnvType;
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;

/**
 * @author yh@firim
 */
public class EnvTypeAdaptor extends YmlAdaptor<PluginEnvType> {

    private static final Collection<PluginEnvType> PLUGIN_ENV_TYPES = ImmutableSet.of(
        PluginEnvType.BOOLEAN,
        PluginEnvType.INTEGER,
        PluginEnvType.STRING,
        PluginEnvType.LIST
    );

    @Override
    public PluginEnvType read(Object obj) {
        return selectEnvType(obj.toString());
    }

    @Override
    public Object write(PluginEnvType s) {
        return null;
    }

    private static PluginEnvType selectEnvType(String type) {
        PluginEnvType selectedType = null;
        for (PluginEnvType pluginEnvType : PLUGIN_ENV_TYPES) {
            if (Objects.equals(pluginEnvType.getValue(), type)) {
                selectedType = pluginEnvType;
                break;
            }
        }
        return selectedType;
    }
}
