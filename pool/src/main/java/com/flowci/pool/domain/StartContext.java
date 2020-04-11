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

package com.flowci.pool.domain;

import java.io.Serializable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public class StartContext implements Serializable {
    
    public static class AgentEnvs {

        public static final String SERVER_URL = "FLOWCI_SERVER_URL";

        public static final String AGENT_TOKEN = "FLOWCI_AGENT_TOKEN";

        public static final String AGENT_LOG_LEVEL = "FLOWCI_AGENT_LOG_LEVEL";

        public static final String AGENT_VOLUMES = "FLOWCI_AGENT_VOLUMES";

        public static final String AGENT_WORKSPACE = "FLOWCI_AGENT_WORKSPACE";

    }

    @NonNull
    private String serverUrl; // ex: http://127.0.0.1:8080

    /**
     * Agent name
     */
    @NonNull
    private String agentName;

    /**
     * Agent token
     */
    @NonNull
    private String token;

    @NonNull
    private String logLevel = "DEBUG";

    public String getDirOnHost() {
        return String.format("${HOME}/.flow.agent-%s", agentName);
    }
}
