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

import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.job.domain.Job;
import com.flowci.domain.Agent;
import com.flowci.domain.Settings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @author yang
 */
public interface AgentService {

    Settings connect(AgentInit initData);

    /**
     * Get agent by id
     */
    Agent get(String id);

    /**
     * Get agent by name
     */
    Agent getByName(String name);

    /**
     * Get agent by token
     */
    Agent getByToken(String token);

    /**
     * Get zookeeper path
     */
    String getPath(Agent agent);

    /**
     * Check agent token is existed
     */
    boolean isExisted(String token);

    /**
     * List agents
     */
    List<Agent> list();

    /**
     * Find agent by status and tags from database
     *
     * @param status Status
     * @param tags Agent tags, optional
     * @throws com.flowci.exception.NotFoundException
     */
    List<Agent> find(Agent.Status status, Set<String> tags);

    /**
     * Delete agent by token
     */
    Agent delete(String token);

    /**
     * Delete agent by object
     */
    void delete(Agent agent);

    /**
     * Set agent tags by token
     */
    Agent setTags(String token, Set<String> tags);

    /**
     * Find available agent and lock
     */
    Optional<Agent> acquire(Job job, Function<String, Boolean> canContinue);

    /**
     * Try to lock agent resource, and set agent status to BUSY
     * @return return agent instance, otherwise return empty
     */
    Optional<Agent> tryLock(String jobId, String agentId);

    /**
     * Release agent, send 'stop' cmd to agent
     */
    void tryRelease(String agentId);

    /**
     * Create agent by name and tags
     */
    Agent create(String name, Set<String> tags, Optional<String> hostId);

    /**
     * Update agent name or and tags
     */
    Agent update(String token, String name, Set<String> tags);

    /**
     * Update agent resource
     */
    Agent update(String token, Agent.Resource resource);

    /**
     * Dispatch cmd to agent
     */
    void dispatch(CmdIn cmd, Agent agent);

}
