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

import com.flow.platform.plugin.domain.envs.PluginPropertyType;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;

/**
 * @author yh@firim
 */
public class EnvTypeAdaptor extends YmlAdaptor<PluginPropertyType> {

    private static final Collection<PluginPropertyType> PLUGIN_ENV_TYPES = ImmutableSet.of(
        PluginPropertyType.BOOLEAN,
        PluginPropertyType.INTEGER,
        PluginPropertyType.STRING,
        PluginPropertyType.LIST
    );

    @Override
    public PluginPropertyType read(Object obj) {
        PluginPropertyType pluginEnvType = selectEnvType(obj.toString());
        if (pluginEnvType == null) {
            throw new PluginException("Yml Env Type Error, Not Found Env Type");
        }
        return pluginEnvType;
    }

    @Override
    public Object write(PluginPropertyType s) {
        return null;
    }

    private static PluginPropertyType selectEnvType(String type) {
        PluginPropertyType selectedType = null;
        for (PluginPropertyType pluginEnvType : PLUGIN_ENV_TYPES) {
            if (Objects.equals(pluginEnvType.getValue(), type)) {
                selectedType = pluginEnvType;
                break;
            }
        }
        return selectedType;
    }
}
