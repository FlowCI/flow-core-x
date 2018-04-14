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

package com.flow.platform.api.service.node;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.user.UserService;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class NodeCrontabServiceImpl implements NodeCrontabService {

    private final static Set<EnvKey> CRONTAB_REQUIRED_ENVS = ImmutableSet.of(
        FlowEnvs.FLOW_TASK_CRONTAB_BRANCH, FlowEnvs.FLOW_TASK_CRONTAB_CONTENT);

    @Autowired
    private Scheduler quartzScheduler;

    @Autowired
    private JobDetail nodeCrontabDetail;

    @Autowired
    private JobService jobService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private UserService userService;

    @Autowired
    public ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void initCrontabTask() {
        taskExecutor.execute(() -> {
            List<Node> flows = nodeService.listFlows(false);
            for (Node flow : flows) {
                try {
                    set(flow);
                } catch (Throwable ignore) {
                }
            }
        });
    }

    @Override
    public void start() {
        try {
            quartzScheduler.start();
        } catch (SchedulerException e) {
            log.warn("Fail to start quartz scheduler: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            quartzScheduler.shutdown(false);
        } catch (SchedulerException e) {
            log.warn("Fail to shutdown quartz scheduler: " + e.getMessage());
        }
    }

    @Override
    public void set(Node node) {
        if (!EnvUtil.hasRequiredEnvKey(node, CRONTAB_REQUIRED_ENVS)) {
            throw new IllegalParameterException("Missing crontab setting env variables");
        }

        final String branch = node.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH);
        final String crontab = node.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT);

        // init and verify cron expression
        CronScheduleBuilder cronSchedule;
        try {
            cronSchedule = CronScheduleBuilder.cronSchedule(crontab);
        } catch (RuntimeException e) {
            throw new IllegalParameterException(e.getMessage());
        }

        // init job if it doesn't exist
        try {
            if (!quartzScheduler.checkExists(nodeCrontabDetail.getKey())) {
                quartzScheduler.addJob(nodeCrontabDetail, true);
            }
        } catch (SchedulerException e) {
            throw new IllegalStatusException("Unable to init quartz job: " + e.getMessage());
        }

        // create data map
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobService", jobService);
        dataMap.put("nodeService", nodeService);
        dataMap.put("userService", userService);
        dataMap.put(KEY_BRANCH, branch);
        dataMap.put(KEY_NODE_PATH, node.getPath());

        // init crontab trigger
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
            .withIdentity(node.getPath())
            .withSchedule(cronSchedule)
            .usingJobData(dataMap)
            .forJob(nodeCrontabDetail)
            .build();

        try {
            quartzScheduler.unscheduleJob(cronTrigger.getKey());
            quartzScheduler.scheduleJob(cronTrigger);
        } catch (SchedulerException e) {
            throw new IllegalStatusException(e.getMessage());
        }
    }

    @Override
    public void delete(Node node) {
        try {
            quartzScheduler.unscheduleJob(new TriggerKey(node.getPath()));
        } catch (SchedulerException e) {
            throw new IllegalStatusException(e.getMessage());
        }
    }

    @Override
    public List<Trigger> triggers() {
        try {
            List<? extends Trigger> triggersOfJob = quartzScheduler.getTriggersOfJob(nodeCrontabDetail.getKey());
            return Lists.newArrayList(triggersOfJob);
        } catch (SchedulerException e) {
            throw new IllegalStatusException(e.getMessage());
        }
    }

    @Override
    public void cleanTriggers() {
        try {
            quartzScheduler.getTriggersOfJob(nodeCrontabDetail.getKey()).clear();
        } catch (SchedulerException e) {
            throw new IllegalStatusException(e.getMessage());
        }
    }
}
