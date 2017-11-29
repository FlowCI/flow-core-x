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

import java.util.List;

/**
 * @author yh@firim
 */
public class PluginEnvKey<T> {

    private String name;

    private PluginEnvType type;

    private T defaultValue;

    private Boolean required;

    private List<String> values;

    public PluginEnvKey(String name, PluginEnvType type, T defaultValue, Boolean required) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PluginEnvType getType() {
        return type;
    }

    public void setType(PluginEnvType type) {
        this.type = type;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
