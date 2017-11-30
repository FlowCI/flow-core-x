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

package com.flow.platform.api.service.job;

import com.flow.platform.api.domain.EnvObject;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;

/**
 * Control center cmd service gateway
 *
 * @author yang
 */
public interface CmdService {

    String DEFAULT_CMD_DIR = "${HOME}/flow-agent-workspace";

    /**
     * Send create session cmd job async
     *
     * @return session id
     * @throws IllegalStatusException throw when cannot create session
     */
    String createSession(Job job, Integer retry);

    /**
     * Send delete session cmd by job
     */
    void deleteSession(Job job);

    /**
     * Send run shell cmd by node for job
     *
     * @return CmdInfo instance
     * @throws IllegalStatusException throw cmd cannot be sent
     */
    CmdInfo runShell(Job job, Node node, String cmdId, EnvObject envVars);

    /**
     * Send shutdown cmd to agent, and shutdown host machine
     *
     * @param path target agent path
     * @param password password for host
     * @throws com.flow.platform.core.exception.IllegalStatusException if unable to send cmd to cc
     */
    void shutdown(AgentPath path, String password);

    /**
     * Close agent
     * @param path target agent path
     */
    void close(AgentPath path);

    /**
     * send cmd to cc
     */
    Cmd sendCmd(CmdInfo cmdInfo, boolean inQueue, int priority);
}
