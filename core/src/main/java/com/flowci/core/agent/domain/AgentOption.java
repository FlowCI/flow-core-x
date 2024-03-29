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

package com.flowci.core.agent.domain;

import com.google.common.base.Strings;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * @author yang
 */
@Data
@Accessors(chain = true)
public class AgentOption {

    @NotEmpty
    private String name;

    private Set<String> tags;

    private String token;

    private int exitOnIdle;

    private String hostId;

    public boolean hasToken() {
        return !Strings.isNullOrEmpty(token);
    }
}
