package com.flowci.core.task.config;

import com.flowci.core.common.helper.ThreadHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskConfig {

    @Bean("localTaskExecutor")
    public ThreadPoolTaskExecutor jobRunExecutor() {
        return ThreadHelper.createTaskExecutor(5, 5, 100, "local-task-");
    }
}
