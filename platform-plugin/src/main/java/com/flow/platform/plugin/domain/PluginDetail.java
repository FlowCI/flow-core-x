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

package com.flow.platform.plugin.domain;

import com.flow.platform.plugin.domain.adaptor.PluginEnvValidator;
import com.flow.platform.plugin.domain.envs.PluginEnvKey;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.gson.annotations.Expose;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yh@firim
 */
public class PluginDetail {

    @Expose
    @YmlSerializer
    private String name;

    @Expose
    @YmlSerializer(name = "properties", validator = PluginEnvValidator.class, required = false)
    private List<PluginEnvKey> inputs;

    @Expose
    @YmlSerializer(required = false)
    private List<String> outputs = new LinkedList<>();

    @Expose
    @YmlSerializer
    private String run;

    @Expose
    @YmlSerializer(required = false)
    private String build;

    public List<PluginEnvKey> getInputs() {
        return inputs;
    }

    public void setInputs(List<PluginEnvKey> inputs) {
        this.inputs = inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginDetail that = (PluginDetail) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PluginDetail{" +
            "name='" + name + '\'' +
            ", inputs=" + inputs +
            ", outputs=" + outputs +
            ", run='" + run + '\'' +
            ", build='" + build + '\'' +
            '}';
    }
}
