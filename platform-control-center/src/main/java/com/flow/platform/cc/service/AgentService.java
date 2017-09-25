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

package com.flow.platform.cc.service;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author gy@fir.im
 */
public interface AgentService {

    int AGENT_SESSION_TIMEOUT_TASK_PERIOD = 60 * 1000; // millisecond

    /**
     * Async update agent offline and online list by zone, will send to agent report queue
     */
    void reportOnline(String zone, Set<String> agents);

    /**
     * List agent by zone name
     */
    List<Agent> list(String zone);

    /**
     * Get online agent set by zone
     */
    List<Agent> listForOnline(String zone);

    /**
     * Find agent by zone and agent name from online list
     *
     * @param key AgentKey object
     * @return Agent object, or null if not found
     */
    Agent find(AgentPath key);

    /**
     * FInd agent by session id
     */
    Agent find(String sessionId);

    /**
     * Find available agent by zone name
     *
     * @return Sorted agent list by updated date
     */
    List<Agent> findAvailable(String zone);

    /**
     * Save agent status and other properties, and send agent webhook
     */
    void saveWithStatus(Agent agent, AgentStatus status);

    /**
     * Is agent session timeout
     */
    boolean isSessionTimeout(Agent agent, ZonedDateTime compareDate, long timeoutInSeconds);

    /**
     * To check agent session timeout
     */
    void sessionTimeoutTask();

    /**
     * Create agent and return token
     */
    Agent create(AgentPath agentPath, String webhook);

    /**
     * refresh token
     */
    String refreshToken(AgentPath agentPath);

    /**
     * Get agent setting by token
     *
     * @param token token
     * @return agent setting
     */
    AgentSettings settings(String token);
}
