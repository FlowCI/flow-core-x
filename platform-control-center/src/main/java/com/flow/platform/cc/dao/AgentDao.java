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

package com.flow.platform.cc.dao;

import com.flow.platform.core.dao.BaseDao;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;

import java.util.List;
import java.util.Set;

/**
 * @author Will
 */
public interface AgentDao extends BaseDao<AgentPath, Agent> {

    /**
     * List agent by zone and status
     *
     * @param zone target zone
     * @param orderByField the field should order by (date fields), null for createdDate
     * @param status expect status list
     * @return list of agent
     */
    List<Agent> list(String zone, String orderByField, AgentStatus... status);

    /**
     * Find agent by path
     *
     * @param agentPath AgentPath object
     * @return Agent instance
     */
    Agent find(AgentPath agentPath);

    /**
     * Find agent by session id
     *
     * @param sessionId session id
     * @return Agent instance
     */
    Agent find(String sessionId);

    /**
     * Batch update agent status by zone
     *
     * @param zone target zone name
     * @param status target status to be updated
     * @param agents target agent name
     * @param isNot indicate NOT in (true) or In(false) agent set
     * @return number of agent updated
     */
    int batchUpdateStatus(String zone, AgentStatus status, Set<String> agents, boolean isNot);
}
