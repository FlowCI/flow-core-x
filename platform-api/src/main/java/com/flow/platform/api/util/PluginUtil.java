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

package com.flow.platform.api.util;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.envs.PluginProperty;
import com.flow.platform.plugin.util.ValidateUtil;
import com.flow.platform.plugin.util.ValidateUtil.Result;
import com.flow.platform.util.CollectionUtil;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
public class PluginUtil {

    public static void validate(List<Node> children, Collection<Plugin> installed) {
        Map<String, Plugin> map = CollectionUtil.toPropertyMap("name", installed);

        for (Node child : children) {
            if (!child.hasPlugin()) {
                continue;
            }

            Plugin plugin = map.get(child.getPlugin());

            if (Objects.isNull(plugin)) {
                throw new IllegalStatusException("Plugin '" + child.getPlugin() + "' does not exist");
            }

            if (Objects.isNull(plugin.getPluginDetail())) {
                throw new IllegalStatusException("Plugin '" + child.getPlugin() + "' missing detail desc");
            }

            List<PluginProperty> properties = plugin.getPluginDetail().getProperties();
            Result result = ValidateUtil.validateProperties(properties, child.getEnvs());

            if (!result.isValid()) {
                throw new IllegalStatusException(result.getError());
            }
        }
    }

}
