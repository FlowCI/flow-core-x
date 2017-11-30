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

import com.google.common.base.Converter;
import com.google.common.base.Strings;

/**
 * @author yh@firim
 */
public enum PluginEnvType {

    STRING("string", new StringHandler()),

    INTEGER("integer", new IntegerHandler()),

    BOOLEAN("boolean", new BooleanHandler()),

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

    public Handler getHandler() {
        return handler;
    }

    public <T> T converter(String value) {
        return (T) handler.converter(value);
    }

    private interface Handler<T> {

        Boolean isValidated(String value);

        T converter(String value);
    }

    private static class StringHandler implements Handler<String> {

        @Override
        public Boolean isValidated(String value) {
            return true;
        }

        @Override
        public String converter(String value) {
            return value;
        }
    }

    private static class IntegerHandler implements Handler<Integer> {

        @Override
        public Boolean isValidated(String value) {
            try {
                integerOfString(value);
            } catch (Throwable throwable) {
                return false;
            }

            return true;
        }

        @Override
        public Integer converter(String value) {
            return integerOfString(value);
        }
    }

    private static class BooleanHandler implements Handler<Boolean> {

        @Override
        public Boolean isValidated(String value) {
            try {
                booleanOfString(value);
            } catch (Throwable throwable) {
                return false;
            }

            return true;
        }

        @Override
        public Boolean converter(String value) {
            return booleanOfString(value);
        }
    }

    private static class ListHandler implements Handler<String> {

        @Override
        public Boolean isValidated(String value) {
            return true;
        }

        @Override
        public String converter(String value) {
            return value;
        }
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
