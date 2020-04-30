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

package com.flowci.domain;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"id"})
public abstract class CmdBase implements Serializable {

    @NonNull
    protected String id;

    @NonNull
    protected String flowId;

    @NonNull
    protected String jobId;

    @NonNull
    protected String nodePath;

    /**
     * Container id if ran from docker
     */
    private String containerId;

    protected boolean allowFailure;
}
