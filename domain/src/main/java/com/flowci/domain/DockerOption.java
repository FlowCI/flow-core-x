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

package com.flowci.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class DockerOption {

    private String image;

    private List<String> entrypoint = Lists.newArrayList("/bin/bash");

    private String networkMode = "host";

    /**
     * List of port like "HOST:CONTAINER 5672:5672"
     */
    private List<String> ports = new LinkedList<>();

    private boolean stopContainer = true;

    private boolean deleteContainer = true;

    @JsonIgnore
    public boolean hasImage() {
        return StringHelper.hasValue(image);
    }
}
