/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.job.service;

import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.event.StepUpdateEvent;
import com.flowci.core.job.event.TaskUpdateEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class PushServiceImpl implements PushService {

    @Autowired
    private String topicForJobs;

    @Autowired
    private String topicForSteps;

    @Autowired
    private SocketPushManager socketPushManager;

    @Override
    @EventListener
    public void onJobCreated(JobCreatedEvent event) {
        Job job = event.getJob();
        socketPushManager.push(topicForJobs, PushEvent.NEW_CREATED, job);
        log.debug("Job created event {} been pushed", job.getId());
    }

    @Override
    @EventListener
    public void onJobStatusChange(JobStatusChangeEvent event) {
        Job job = event.getJob();
        socketPushManager.push(topicForJobs, PushEvent.STATUS_CHANGE, job);
        log.debug("Job {} with status {} been pushed", job.getId(), job.getStatus());
    }

    @Override
    @EventListener
    public void onStepStatusChange(StepUpdateEvent event) {
        String topic = topicForSteps + "/" + event.getJobId();
        if (event.isInit()) {
            socketPushManager.push(topic, PushEvent.NEW_CREATED, event.getItems());
            return;
        }
        socketPushManager.push(topic, PushEvent.STATUS_CHANGE, event.getItems());
    }

    @Override
    @EventListener
    public void onTaskStatusChange(TaskUpdateEvent event) {

    }
}
