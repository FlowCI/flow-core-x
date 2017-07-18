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

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;

import java.util.List;
import java.util.Set;

/**
 * @author Will
 */
public interface CmdDao extends BaseDao<String, Cmd> {

    /**
     * List cmd by agent path, cmd type and cmd status
     *
     * @param agentPath nullable, zone or agent name also nullable
     * @param types nullable, select in types
     * @param status nullable, select in status
     */
    List<Cmd> list(AgentPath agentPath, Set<CmdType> types, Set<CmdStatus> status);
}
