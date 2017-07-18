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

import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author gy@fir.im
 */
@Configuration
public class TaskConfig {

    private final static Logger LOGGER = new Logger(TaskConfig.class);

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
        LOGGER.trace("Zone.Toggle.KeepIdleAgent: %s", enableKeepIdleAgentTask);
        LOGGER.trace("Agent.Toggle.SessionTimeout: %s", enableAgentSessionTimeoutTask);
        LOGGER.trace("Cmd.Toggle.ExecutionTimeout: %s", enableCmdExecTimeoutTask);
        LOGGER.trace("Mos.Toggle.Clean: %s", enableMosCleanTask);
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
