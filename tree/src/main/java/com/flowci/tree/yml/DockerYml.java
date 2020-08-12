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

package com.flowci.tree.yml;

import com.flowci.domain.DockerOption;
import com.flowci.util.ObjectsHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
public class DockerYml {

    private String image;

    private String name;

    private String network_mode;

    private List<String> ports;

    private List<String> entrypoint;

    private List<String> command;

    private Map<String, String> environment;

    private Boolean is_runtime;

    private Boolean stop_on_finish;

    private Boolean delete_on_finish;

    public DockerOption toDockerOption() {
        Objects.requireNonNull(image, "Docker image must be specified");

        DockerOption option = new DockerOption();
        option.setImage(image);

        ObjectsHelper.ifNotNull(name, option::setName);
        ObjectsHelper.ifNotNull(network_mode, option::setNetworkMode);
        ObjectsHelper.ifNotNull(ports, option::setPorts);
        ObjectsHelper.ifNotNull(entrypoint, option::setEntrypoint);
        ObjectsHelper.ifNotNull(command, option::setCommand);
        ObjectsHelper.ifNotNull(environment, option::setEnvironment);
        ObjectsHelper.ifNotNull(is_runtime, option::setRuntime);
        ObjectsHelper.ifNotNull(stop_on_finish, option::setStopContainer);
        ObjectsHelper.ifNotNull(delete_on_finish, option::setDeleteContainer);

        return option;
    }
}
