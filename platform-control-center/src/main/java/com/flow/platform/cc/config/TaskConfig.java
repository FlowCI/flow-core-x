package com.flow.platform.cc.config;

import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Created by gy@fir.im on 15/06/2017.
 * Copyright fir.im
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
