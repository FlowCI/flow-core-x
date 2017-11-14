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

package com.flow.platform.api.config;

import com.flow.platform.api.task.NodeCrontabTask;
import com.flow.platform.core.exception.IllegalStatusException;
import java.io.IOException;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @author yang
 */
@Configurable
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    private final static int DEFAULT_SCHEDULER_POOL_SIZE = 10;

    /**
     * Thread pool for spring scheduling
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler schedulerTaskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(DEFAULT_SCHEDULER_POOL_SIZE);
        scheduler.setDaemon(true);
        scheduler.setThreadNamePrefix("api-task-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Setup quartz scheduler
     */
    @Bean
    public Scheduler quartzScheduler() {
        try {
            StdSchedulerFactory factory = new StdSchedulerFactory();
            factory.initialize(new ClassPathResource("quartz.properties").getInputStream());
            return factory.getScheduler();
        } catch (SchedulerException | IOException e) {
            throw new IllegalStatusException("Unable to init quartz: " + e.getMessage());
        }
    }

    /**
     * Setup flow crontab job detail
     */
    @Bean
    public JobDetail nodeCrontabDetail() {
        return JobBuilder.newJob(NodeCrontabTask.class).storeDurably(true).build();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(schedulerTaskExecutor());
    }
}
