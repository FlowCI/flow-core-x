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
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.exception.IllegalStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * @author gy@fir.im
 */
public interface CmdService {

    /**
     * Create command from CmdInfo
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
     * Send cmd id to agent and update cmd target agent path
     * - AgentPath,
     * - 'zone' field is required
     * - 'name' field is optional
     * - which mean system will automatic select idle agent to send
     * throw AgentErr.NotAvailableException if no idle agent and update cmd status to CmdStatus.Reject
     *
     * @param cmdId cmd id it will load from dao
     * @param shouldResetStatus should reset cmd status to PENDING
     * @return command objc with id
     * @throws AgentErr.NotAvailableException if agent busy
     * @throws com.flow.platform.util.zk.ZkException.NotExitException if no zk node exist
     * @throws IllegalParameterException if cmd not found
     * @throws IllegalStatusException if cmd status is in finished status
     */
    Cmd send(String cmdId, boolean shouldResetStatus);

    /**
     * Wrapper of send(cmdId), it includes create cmd by CmdInfo
     */
    Cmd send(CmdInfo cmdInfo);

    /**
     * Send cmd info to queue
     */
    Cmd queue(CmdInfo cmdInfo);

    /**
     * Check cmd is timeout
     *
     * @return timeout or not
     */
    boolean isTimeout(Cmd cmd);

    /**
     * Update cmd status and result, send cmd webhook if existed
     *
     * @param cmdId target cmd id
     * @param status target cmd status should updated
     * @param result CmdResult, nullable
     * @param updateAgentStatus should update agent status according to cmd status
     * @param callWebhook should invoke webhook
     */
    void updateStatus(String cmdId, CmdStatus status, CmdResult result, boolean updateAgentStatus, boolean callWebhook);

    /**
     * Reset cmd status to init status PENDING
     */
    void resetStatus(String cmdId);

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
