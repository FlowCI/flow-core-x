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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flowci.core.common.domain.SourceWithDomain;
import com.flowci.core.flow.domain.StatsType;
import com.flowci.domain.DockerOption;
import com.flowci.domain.Input;
import com.flowci.domain.Version;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Document(collection = "plugins")
@ToString(of = {"name", "version", "branch"})
public class Plugin extends SourceWithDomain {

    /**
     * Metadata from YAML plugin file
     */
    @Data
    public static class Meta {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public interface RestResponse {

            @JsonIgnore
            String getName();

            @JsonIgnore
            String getVersion();

            @JsonIgnore
            List<Input> getInputs();

            @JsonIgnore
            Set<String> getExports();

            @JsonIgnore
            List<StatsType> getStatsTypes();

            @JsonIgnore
            boolean isAllowFailure();

            @JsonIgnore
            String getBash();

            @JsonIgnore
            String getPwsh();
        }


        private String name;

        private Version version;

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
    }

    @Id
    private String id;

    private boolean synced;

    private Date syncTime;

    // the following properties should be loaded from plugin repository.json file

    @Indexed(name = "index_plugins_name", unique = true)
    private String name;

    private String branch = "master";

    private String description;

    private Set<String> tags = new HashSet<>();

    private String author;

    private Version version;

    private Meta meta;

    public Plugin(String name, Version version) {
        this.setName(name);
        this.setVersion(version);
    }
}
