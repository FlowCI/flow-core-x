/*
 * Copyright 2020 flow.ci
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

package com.flowci.domain.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class DockerOption implements Serializable {

    private String image;

    private String auth;

    private String name;

    private List<String> entrypoint;

    private List<String> command;

    private String network;

    private String user = "root"; // default user

    private Map<String, String> environment = new HashMap<>();

    @JsonProperty("isRuntime")
    private boolean runtime = false;

    /**
     * List of port like "HOST:CONTAINER 5672:5672"
     */
    private List<String> ports = new LinkedList<>();

    @JsonProperty("isStopContainer")
    private boolean stopContainer = true;

    @JsonProperty("isDeleteContainer")
    private boolean deleteContainer = true;

    private String containerId;

    @JsonIgnore
    public boolean hasName() {
        return StringHelper.hasValue(name);
    }
}
