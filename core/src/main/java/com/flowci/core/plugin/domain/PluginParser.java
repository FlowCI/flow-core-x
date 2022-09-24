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

import com.flowci.core.flow.domain.MatrixType;
import com.flowci.domain.Input;
import com.flowci.domain.VarType;
import com.flowci.domain.Version;
import com.flowci.tree.yml.DockerYml;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.flowci.util.YamlHelper;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author yang
 */
public abstract class PluginParser {

    public static Plugin.Meta parse(InputStream is) {
        Yaml yaml = YamlHelper.create(PluginWrapper.class);
        PluginWrapper load = yaml.load(is);
        return load.toMeta();
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

        public String bash;

        public String pwsh;

        public String script;

        public DockerYml docker;

        public Plugin.Meta toMeta() {
            Plugin.Meta meta = new Plugin.Meta();
            meta.setName(name);
            meta.setVersion(Version.parse(version));
            meta.setIcon(icon);
            meta.setBash(bash);
            meta.setPwsh(pwsh);

            ObjectsHelper.ifNotNull(docker, val -> meta.setDocker(val.toDockerOption()));
            ObjectsHelper.ifNotNull(exports, meta::setExports);
            ObjectsHelper.ifNotNull(allow_failure, meta::setAllowFailure);
            ObjectsHelper.ifNotNull(stats, list -> {
                for (StatsWrapper wrapper : list) {
                    meta.getMatrixTypes().add(wrapper.toStatsType());
                }
            });
            ObjectsHelper.ifNotNull(inputs, list -> {
                for (VariableWrapper wrapper : list) {
                    meta.getInputs().add(wrapper.toVariable());
                }
            });

            // backward compatible, set script to bash
            if (StringHelper.hasValue(script)) {
                if (!StringHelper.hasValue(bash)) {
                    meta.setBash(script);
                }
            }

            return meta;
        }
    }

    @NoArgsConstructor
    private static class StatsWrapper {

        public String name;

        public String desc;

        public boolean percent;

        public List<String> fields = new LinkedList<>();

        public MatrixType toStatsType() {
            return new MatrixType()
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
