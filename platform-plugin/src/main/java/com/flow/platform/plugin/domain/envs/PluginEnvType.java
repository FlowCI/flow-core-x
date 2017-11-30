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
import java.util.List;
import java.util.Objects;

/**
 * @author yh@firim
 */
public enum PluginEnvType {

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

    private Handler handler;

    PluginEnvType(String value, Handler handler) {
        this.value = value;
        this.handler = handler;
    }

    public String getValue() {
        return value;
    }

    public <T> T converter(PluginEnvKey pluginEnvKey) {
        return (T) handler.converter(pluginEnvKey);
    }

    public Boolean isValidated(PluginEnvKey pluginEnvKey) {
        return handler.isValidated(pluginEnvKey);
    }

    private interface Handler<T> {

        Boolean isValidated(PluginEnvKey pluginEnvKey);

        T converter(PluginEnvKey pluginEnvKey);
    }

    private static class StringHandler implements Handler<String> {

        @Override
        public Boolean isValidated(PluginEnvKey pluginEnvKey) {
            return true;
        }

        @Override
        public String converter(PluginEnvKey pluginEnvKey) {
            return pluginEnvKey.getDefaultValue();
        }
    }

    private static class IntegerHandler implements Handler<Integer> {

        @Override
        public Boolean isValidated(PluginEnvKey pluginEnvKey) {
            try {
                integerOfString(pluginEnvKey.getDefaultValue());
            } catch (Throwable throwable) {
                return false;
            }

            return true;
        }

        @Override
        public Integer converter(PluginEnvKey pluginEnvKey) {
            return integerOfString(pluginEnvKey.getDefaultValue());
        }
    }

    private static class BooleanHandler implements Handler<Boolean> {

        @Override
        public Boolean isValidated(PluginEnvKey pluginEnvKey) {

            if (Strings.isNullOrEmpty(pluginEnvKey.getDefaultValue())) {
                return true;
            }

            if (Objects.equals("true", pluginEnvKey.getDefaultValue().toLowerCase())) {
                return true;
            }

            if (Objects.equals("false", pluginEnvKey.getDefaultValue().toLowerCase())) {
                return true;
            }

            return false;
        }

        @Override
        public Boolean converter(PluginEnvKey pluginEnvKey) {
            return booleanOfString(pluginEnvKey.getDefaultValue());
        }
    }

    private static class ListHandler implements Handler<String> {

        @Override
        public Boolean isValidated(PluginEnvKey pluginEnvKey) {
            List<String> values = pluginEnvKey.getValues();

            if (Objects.isNull(values) || values.isEmpty()) {
                return false;
            }

            if (Strings.isNullOrEmpty(pluginEnvKey.getDefaultValue())) {
                return true;
            }

            if (!values.contains(pluginEnvKey.getDefaultValue())) {
                return false;
            }

            return true;
        }

        @Override
        public String converter(PluginEnvKey pluginEnvKey) {
            return pluginEnvKey.getDefaultValue();
        }
    }

    private static boolean booleanOfString(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        if (Objects.equals("true", value.toLowerCase())) {
            return true;
        }

        if (Objects.equals("false", value.toLowerCase())) {
            return false;
        }

        return false;
    }

    private static Integer integerOfString(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        } else {
            return Integer.valueOf(value);
        }
    }

}
