package com.flow.platform.cc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by gy@fir.im on 15/06/2017.
 * Copyright fir.im
 */
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
