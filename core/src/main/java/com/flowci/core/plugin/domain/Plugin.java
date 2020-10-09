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
import com.flowci.domain.DockerOption;
import com.flowci.domain.Input;
import com.flowci.domain.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Document(collection = "plugins")
public class Plugin extends PluginRepoInfo {

    @Id
    private String id;

    private List<Input> inputs = new LinkedList<>();

    // output env var name which will write to job context
    private Set<String> exports = new HashSet<>();

    // Plugin that supported statistic types
    private List<StatsType> statsTypes = new LinkedList<>();

    private boolean allowFailure;

    private DockerOption docker;

    private String bash;

    private String pwsh;

    private String icon;

    public Plugin(String name, Version version) {
        this.setName(name);
        this.setVersion(version);
    }

    public void update(Plugin src) {
        this.setVersion(src.getVersion());
        this.setIcon(src.icon);
        this.setInputs(src.inputs);
        this.setStatsTypes(src.statsTypes);
        this.setAllowFailure(src.allowFailure);
        this.setBash(src.bash);
        this.setPwsh(src.pwsh);
        this.setDocker(src.docker);
    }
}
