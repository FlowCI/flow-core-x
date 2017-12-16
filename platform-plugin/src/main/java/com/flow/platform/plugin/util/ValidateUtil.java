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

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginDetail;
import com.flow.platform.plugin.domain.PluginWithProperties;
import com.flow.platform.plugin.domain.envs.PluginProperty;
import com.flow.platform.util.CollectionUtil;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
public class ValidateUtil {

    public static class Result {

        private final boolean isValid;

        private final String error;

        public Result(boolean isValid, String error) {
            this.isValid = isValid;
            this.error = error;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Validate plugin name and value which are match installed plugin list
     *
     * @param nameWithValues Plugin name with property-value
     * @param installed Installed plugin list
     * @return Result
     */
    public static Result validatePlugin(List<PluginWithProperties> nameWithValues, List<Plugin> installed) {
        final Map<String, Plugin> map = CollectionUtil.toPropertyMap("name", installed);

        for (PluginWithProperties item : nameWithValues) {
            final String pluginName = item.getName();
            final Map<String, String> propertyWithValue = item.getProperties();

            Plugin plugin = map.get(pluginName);
            if (Objects.isNull(plugin)) {
                final String message = String.format("The plugin '%s' is not available", pluginName);
                return new Result(false, message);
            }

            PluginDetail detail = plugin.getPluginDetail();
            if (Objects.isNull(detail)) {
                return new Result(false, "Illegal plugin detail description: " + pluginName);
            }

            Result result = validateProperties(detail.getProperties(), propertyWithValue);
            if (!result.isValid) {
                return result;
            }
        }

        return new Result(true, null);
    }

    /**
     * Validate keyValues according to properties
     *
     * @param definitions The origin property definition
     * @param propertyWithValue the key value map from input, key as property name
     */
    public static Result validateProperties(List<PluginProperty> definitions, Map<String, String> propertyWithValue) {
        for (PluginProperty propertyDef : definitions) {
            String value = propertyWithValue.get(propertyDef.getName());

            if (propertyDef.getRequired() && Strings.isNullOrEmpty(value)) {
                final String message = String.format("The property '%s' is missing", propertyDef.getName());
                return new Result(false, message);
            }

            if (!propertyDef.getRequired() && Strings.isNullOrEmpty(value)) {
                continue;
            }

            if (!propertyDef.validate(value)) {
                final String message = String.format("The property '%s' is illegal", propertyDef.getName());
                return new Result(false, message);
            }
        }

        return new Result(true, null);
    }

}
