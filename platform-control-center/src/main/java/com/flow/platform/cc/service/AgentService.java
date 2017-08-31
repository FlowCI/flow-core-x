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
     * Get online agent set by zone
     */
    List<Agent> onlineList(String zone);

    List<Agent> list(String zone);

    /**
     * Find agent by zone and agent name from online list
     *
     * @param key AgentKey object
     * @return Agent object, or null if not found
     */
    Agent find(AgentPath key);

    /**
     * shutdown agent
     */
    Boolean shutdown(AgentPath agentPath, String password);

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
     * Create agent session, set existed session id to agent
     * It will auto create session id if existedSessionId is null
     *
     * @param agent target agent
     * @param existSessionId the session id set to agent or null
     * @return session id or null if unable to create session
     */
    String createSession(Agent agent, String existSessionId);

    /**
     * Check has running cmd and delete agent session
     */
    void deleteSession(Agent agent);

    /**
     * Update agent status
     */
    void updateStatus(AgentPath path, AgentStatus status);

    /**
     * Is agent session timeout
     */
    boolean isSessionTimeout(Agent agent, ZonedDateTime compareDate, long timeoutInSeconds);

    /**
     * To check agent session timeout
     */
    void sessionTimeoutTask();

}
