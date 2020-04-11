/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.plugin.domain;

import com.flowci.core.flow.domain.StatsType;
import com.flowci.domain.VarType;
import com.flowci.domain.Version;
import com.flowci.tree.yml.DockerYml;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.YamlHelper;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.omg.CORBA.ObjectHelper;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * @author yang
 */
public class PluginParser {

    public static Plugin parse(InputStream is) {
        Yaml yaml = YamlHelper.create(PluginWrapper.class);
        PluginWrapper load = yaml.load(is);
        return load.toPlugin();
    }

    @NoArgsConstructor
    private static class PluginWrapper {

        @NonNull
        public String name;

        @NonNull
        public String version;

        public String icon;

        public List<VariableWrapper> inputs;

        public Set<String> exports;

        public List<StatsWrapper> stats;

        public Boolean allow_failure;

        public String script;

        public ParentWrapper parent;

        public DockerYml docker;

        public Plugin toPlugin() {
            Plugin plugin = new Plugin(name, Version.parse(version));
            plugin.setIcon(icon);

            if (Objects.isNull(parent)) {
                plugin.setBody(new ScriptBody(script));
            } else {
                plugin.setBody(parent.toParentBody());
            }

            ObjectsHelper.ifNotNull(docker, val -> plugin.setDocker(val.toDockerOption()));
            ObjectsHelper.ifNotNull(exports, plugin::setExports);
            ObjectsHelper.ifNotNull(allow_failure, plugin::setAllowFailure);
            ObjectsHelper.ifNotNull(stats, list -> {
                for (StatsWrapper wrapper : list) {
                    plugin.getStatsTypes().add(wrapper.toStatsType());
                }
            });
            ObjectsHelper.ifNotNull(inputs, list -> {
                for (VariableWrapper wrapper : list) {
                    plugin.getInputs().add(wrapper.toVariable());
                }
            });

            return plugin;
        }
    }

    @NoArgsConstructor
    private static class ParentWrapper {

        @NonNull
        public String name;

        @NonNull
        public String version;

        public Map<String, String> envs = new LinkedHashMap<>();

        public PluginBody toParentBody() {
            ParentBody body = new ParentBody();
            body.setName(name);
            body.setVersion(version);
            body.getEnvs().putAll(envs);
            return body;
        }
    }

    @NoArgsConstructor
    private static class StatsWrapper {

        public String name;

        public String desc;

        public boolean percent;

        public List<String> fields = new LinkedList<>();

        public StatsType toStatsType() {
            return new StatsType()
                    .setName(name)
                    .setDesc(desc)
                    .setPercent(percent)
                    .setFields(fields);
        }
    }

    @NoArgsConstructor
    private static class VariableWrapper {

        @NonNull
        public String name;

        public String alias;

        @NonNull
        public String type;

        @NonNull
        public Boolean required;

        // default value
        public String value;

        public Input toVariable() {
            Input var = new Input(name, VarType.valueOf(type.toUpperCase()));
            var.setRequired(required);
            var.setAlias(alias);
            var.setValue(value);
            return var;
        }
    }

}
