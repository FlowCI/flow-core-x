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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.flowci.domain.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"name"})
@ToString(of = {"name", "version", "source", "branch"})
public class PluginRepoInfo implements Serializable {

    @Indexed(name = "index_plugins_name", unique = true)
    private String name;

    private String source; // git repo url

    @JsonAlias("source_cn")
    private String sourceCn; // git repo url for cn

    private String branch = "master";

    private String description;

    private Set<String> tags = new HashSet<>();

    private String author;

    private Version version;
}
