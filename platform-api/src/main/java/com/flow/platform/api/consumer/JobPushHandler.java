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
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * @author yang
 */
public abstract class JobPushHandler {

    @Autowired
    protected SimpMessagingTemplate template;

    @Autowired
    protected RawGsonMessageConverter jsonConverter;

    @Autowired
    protected JobService jobService;

    protected void push(BigInteger jobId) {
        Job job = jobService.find(jobId);
        String jobInJson = jsonConverter.getGsonForWriter().toJson(job);
        String jobTopic = jobEventBuilder(job);
        template.convertAndSend(jobTopic, jobInJson);
    }

    /**
     * Generate job event topic path, /topic/job/{node path}-{build number}
     */
    protected static String jobEventBuilder(Job job) {
        return String.format("%s/%s-%s", WebSocketConfig.TOPIC_FOR_JOB, job.getNodePath(), job.getNumber());
    }
}
