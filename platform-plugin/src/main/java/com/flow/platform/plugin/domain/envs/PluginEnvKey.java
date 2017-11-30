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

import com.flow.platform.plugin.domain.adaptor.EnvTypeAdaptor;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * @author yh@firim
 */
public class PluginEnvKey {

    @Expose
    @YmlSerializer
    private String name;

    @Expose
    @YmlSerializer(adaptor = EnvTypeAdaptor.class)
    private PluginEnvType type;

    @Expose
    @YmlSerializer(name = "default", required = false)
    private String defaultValue;

    @Expose
    @YmlSerializer(required = false)
    private Boolean required;

    @Expose
    @YmlSerializer(required = false)
    private List<String> values;


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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public <T> T converter() {
        return getType().converter(defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginEnvKey that = (PluginEnvKey) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PluginEnvKey{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", defaultValue=" + defaultValue +
            ", required=" + required +
            ", values=" + values +
            '}';
    }
}
