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

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * @author gy@fir.im
 */
public interface CmdService {

    /**
     * Create command from CmdBase
     *
     * @return Cmd objc with id
     */
    Cmd create(CmdInfo cmd);

    /**
     * Find cmd obj by id
     *
     * @return Cmd object
     */
    Cmd find(String cmdId);

    /**
     * List cmd by agent path, order by cmd create date
     */
    List<Cmd> listByAgentPath(AgentPath agentPath);

    /**
     * List cmd by zone, order by cmd create date
     */
    List<Cmd> listByZone(String zone);

    /**
     * List cmd result by ids
     */
    List<CmdResult> listResult(Set<String> cmdIds);

    /**
     * Send CmdInfo with AgentPath which to identify where is cmd destination
     * - AgentPath,
     * - 'zone' field is required
     * - 'name' field is optional
     * - which mean system will automatic select idle agent to send
     * throw AgentErr.NotAvailableException if no idle agent and update cmd status to CmdStatus.Reject
     *
     * @return command objc with id
     * @throws AgentErr.NotAvailableException if agent busy
     */
    Cmd send(CmdInfo cmdInfo);

    /**
     * Check cmd is timeout
     *
     * @return timeout or not
     */
    boolean isTimeout(Cmd cmd);

    /**
     * Update cmd status and result
     *
     * @param cmdId target cmd id
     * @param status target cmd status should updated
     * @param result CmdResult, nullable
     * @param updateAgentStatus should update agent status according to cmd status
     */
    void updateStatus(String cmdId, CmdStatus status, CmdResult result, boolean updateAgentStatus);

    /**
     * Record full zipped log to store
     */
    void saveLog(String cmdId, MultipartFile file);

    /**
     * Invoke webhook url to report Cmd
     */
    void webhookCallback(CmdBase cmdBase);

    /**
     * Check timeout cmd by created date for all busy agent
     */
    void checkTimeoutTask();
}
