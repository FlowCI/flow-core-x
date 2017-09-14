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

package com.flow.platform.api.consumer;

import com.flow.platform.api.config.WebSocketConfig;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.events.JobStatusChangeEvent;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * To handle JobStatusChangeEvent and NodeResultStatusChangeEvent
 *
 * @author yang
 */
public class JobEventPushHandler implements ApplicationListener<JobStatusChangeEvent> {

    private final static Logger LOGGER = new Logger(JobEventPushHandler.class);

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private RawGsonMessageConverter jsonConverter;

    @Autowired
    private JobService jobService;

    @Override
    public void onApplicationEvent(JobStatusChangeEvent event) {
        LOGGER.debugMarker("JobStatusChangeEvent",
            "Job %s event from %s to %s", event.getJobId(), event.getFrom(), event.getTo());

        Job job = jobService.find(event.getJobId());
        String jobInJson = jsonConverter.getGsonForWriter().toJson(job);
        String jobTopic = jobEventBuilder(job);
        template.convertAndSend(jobTopic, jobInJson);
    }

    /**
     * Generate job event topic path, /topic/job/{node path}-{build number}
     */
    private static String jobEventBuilder(Job job) {
        return String.format("%s/%s-%s", WebSocketConfig.TOPIC_FOR_JOB, job.getNodePath(), job.getNumber());
    }
}
