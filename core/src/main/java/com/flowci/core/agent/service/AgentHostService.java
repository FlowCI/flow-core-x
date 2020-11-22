/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flowci.core.agent.service;

import com.flowci.core.agent.domain.AgentHost;

import java.util.List;

public interface AgentHostService {

    /**
     * Create an agent host
     */
    AgentHost createOrUpdate(AgentHost host);

    /**
     * Disable agent host
     */
    AgentHost disable(String name);

    /**
     * Delete agent host, and clean up all related agent containers
     */
    AgentHost delete(String name);

    /**
     * Start agent on the host
     */
    boolean start(AgentHost host);

    /**
     * Sync agents between db and host
     */
    void sync(AgentHost host);

    /**
     * Num of agent on the host
     */
    int size(AgentHost host);

    /**
     * Stop or remove unused agent
     */
    void collect(AgentHost host);

    /**
     * Remove all docker container
     */
    void removeAll(AgentHost host);

    /**
     * Test agent host connection
     */
    void testConn(AgentHost host);

    /**
     * List all agent hosts
     */
    List<AgentHost> list();

    /**
     * Get agent host by name
     */
    AgentHost get(String name);
}