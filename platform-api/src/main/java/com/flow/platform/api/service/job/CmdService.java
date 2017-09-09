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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.domain.AgentPath;

/**
 * Control center cmd service
 *
 * @author yang
 */
public interface CmdService {

    /**
     * Send create session cmd job async
     *
     * @return session id
     */
    String createSession(Job job);

    /**
     * Send delete session cmd by job
     */
    void deleteSession(Job job);

    /**
     * Send run shell cmd by node for job
     */
    void runShell(Job job, Node node, String cmdId);

    /**
     * Send shutdown cmd to agent
     *
     * @param path target agent path
     * @param password password for host
     * @throws com.flow.platform.core.exception.IllegalStatusException if unable to send cmd to cc
     */
    void shutdown(AgentPath path, String password);
}
