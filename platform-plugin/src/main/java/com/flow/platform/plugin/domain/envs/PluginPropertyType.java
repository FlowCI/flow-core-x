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

package com.flow.platform.plugin.domain.envs;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * @author yh@firim
 */
public enum PluginPropertyType {

    /**
     * ENV Type String
     */
    STRING("string", new StringHandler()),

    /**
     * ENV Type Integer
     */
    INTEGER("integer", new IntegerHandler()),

    /**
     * ENV Type Boolean
     */
    BOOLEAN("boolean", new BooleanHandler()),

    /**
     * ENV Type List
     */
    LIST("list", new ListHandler());


    private String value;

    private TypeHandler handler;

    PluginPropertyType(String value, TypeHandler handler) {
        this.value = value;
        this.handler = handler;
    }

    public String getValue() {
        return value;
    }

    public <T> TypeHandler<T> getHandler() {
        return handler;
    }

    public static abstract class TypeHandler<T> {

        public Boolean isValidated(PluginProperty pluginEnvKey, String value) {
            if (pluginEnvKey.getRequired() && Strings.isNullOrEmpty(value)) {
                return false;
            }

            return doValidate(pluginEnvKey, value);
        }

        abstract boolean doValidate(PluginProperty pluginEnvKey, String value);

        public abstract T convert(PluginProperty pluginEnvKey, String value);
    }

    private static class StringHandler extends TypeHandler<String> {

        @Override
        boolean doValidate(PluginProperty pluginEnvKey, String value) {
            return true;
        }

        @Override
        public String convert(PluginProperty pluginEnvKey, String value) {
            return value;
        }
    }

    private static class IntegerHandler extends TypeHandler<Integer> {

        @Override
        boolean doValidate(PluginProperty pluginEnvKey, String value) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public Integer convert(PluginProperty pluginEnvKey, String value) {
            return Integer.parseInt(value);
        }
    }

    private static class BooleanHandler extends TypeHandler<Boolean> {

        private final Set<String> values = ImmutableSet.of("true", "false", "TRUE", "FALSE");

        @Override
        boolean doValidate(PluginProperty pluginEnvKey, String value) {
            try {
                return values.contains(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public Boolean convert(PluginProperty pluginEnvKey, String value) {
            return Boolean.parseBoolean(value);
        }
    }

    private static class ListHandler extends TypeHandler<String> {

        @Override
        boolean doValidate(PluginProperty pluginEnvKey, String value) {
            return pluginEnvKey.getValues().contains(value);
        }

        @Override
        public String convert(PluginProperty pluginEnvKey, String value) {
            return value;
        }
    }
}
