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

package com.flowci.core.agent.service;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentProfile;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.CreateOrUpdateAgent;
import com.flowci.tree.Selector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yang
 */
public interface AgentService {

    /**
     * Get agent by id
     */
    Agent get(String id);

    /**
     * Get agent profile by token
     */
    AgentProfile getProfile(String token);

    /**
     * Get agent by name
     */
    Agent getByName(String name);

    /**
     * Get agent by token
     */
    Agent getByToken(String token);

    /**
     * Check agent token is existed
     */
    boolean isExisted(String token);

    /**
     * List agents
     */
    List<Agent> list();

    /**
     * List agent by given ids
     */
    Iterable<Agent> list(Collection<String> ids);

    /**
     * Delete agent by token
     */
    Agent delete(String token);

    /**
     * Delete agent by object
     */
    void delete(Agent agent);

    /**
     * Find available agent and set to busy, atomic
     */
    Optional<Agent> acquire(String jobId, Selector selector);

    /**
     * Acquire job id to specific agent and set to busy, atomic
     */
    Optional<Agent> acquire(String jobId, Selector selector, String agentId, boolean shouldIdle);

    /**
     * Release agent, set to IDLE, atomic
     */
    void release(Collection<String> ids);

    /**
     * Create agent by name and tags
     */
    Agent create(CreateOrUpdateAgent option);

    /**
     * Update agent name or and tags
     */
    Agent update(CreateOrUpdateAgent option);

    /**
     * Update agent status
     */
    Agent update(Agent agent, Agent.Status status);

    /**
     * Dispatch cmd to agent
     */
    void dispatch(CmdIn cmd, Agent agent);

}
