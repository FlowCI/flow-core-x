/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.AgentWithFlow;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import java.util.List;

/**
 * @author yh@firim
 */
public interface AgentService {

    List<AgentWithFlow> list();

    /**
     * agent shutdown
     *
     * @param zone required
     * @param name required
     * @param password required
     * @return true or false
     */
    Boolean shutdown(String zone, String name, String password);

    /**
     * Create agent and return token from cc
     */
    Agent create(AgentPath agentPath);

    /**
     * Get agent setting by token from cc
     */
    AgentSettings settings(String token);

    /**
     * Delete agent by agentPath from cc
     */
    void delete(AgentPath agentPath);
    /**
     * send sys cmd
     * @param agentPath required
     */
    void sendSysCmd(AgentPath agentPath);
}
