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

package com.flow.platform.cc.config;

import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author gy@fir.im
 */
@Log4j2
@Configuration
public class TaskConfig {

    @Value("${task.zone.toggle.keep_idle_agent}")
    private boolean enableKeepIdleAgentTask;

    @Value("${task.agent.toggle.session_timeout}")
    private boolean enableAgentSessionTimeoutTask;

    @Value("${task.cmd.toggle.execution_timeout}")
    private boolean enableCmdExecTimeoutTask;

    @Value("${task.instance.mos.toggle.clean}")
    private boolean enableMosCleanTask;

    @PostConstruct
    public void init() {
        log.trace("Zone.Toggle.KeepIdleAgent: {}", enableKeepIdleAgentTask);
        log.trace("Agent.Toggle.SessionTimeout: {}", enableAgentSessionTimeoutTask);
        log.trace("Cmd.Toggle.ExecutionTimeout: {}", enableCmdExecTimeoutTask);
        log.trace("Mos.Toggle.Clean: {}", enableMosCleanTask);
    }

    public boolean isEnableKeepIdleAgentTask() {
        return enableKeepIdleAgentTask;
    }

    public boolean isEnableAgentSessionTimeoutTask() {
        return enableAgentSessionTimeoutTask;
    }

    public boolean isEnableCmdExecTimeoutTask() {
        return enableCmdExecTimeoutTask;
    }

    public boolean isEnableMosCleanTask() {
        return enableMosCleanTask;
    }
}
