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

import com.flow.platform.plugin.domain.envs.PluginProperty;
import com.flow.platform.plugin.exception.PluginPropertyException;
import com.flow.platform.util.CollectionUtil;
import java.util.HashMap;
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
     * Validate keyValues according to properties
     *
     * @param definition The origin property definition
     * @param keyValues the key value map from input, key as property name
     */
    public static Result validate(List<PluginProperty> definition, Map<String, String> keyValues) {
        Map<Object, PluginProperty> map = CollectionUtil.toPropertyMap("name", definition);

        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            PluginProperty property = map.get(entry.getKey());

            if (Objects.isNull(property)) {
                final String message = String.format("The property '%s' is not defined", entry.getKey());
                return new Result(false, message);
            }

            if (!property.validate(entry.getValue())) {
                final String message = String.format("The property '%s' is illegal", entry.getKey());
                return new Result(false, message);
            }
        }

        return new Result(true, null);
    }

}
