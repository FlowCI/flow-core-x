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
import com.flow.platform.api.push.PushHandler;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.core.exception.NotFoundException;
import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public abstract class JobEventPushHandler extends PushHandler {

    @Autowired
    private JobService jobService;

    protected void push(BigInteger jobId) {
        Job job = jobService.find(jobId);
        String jobTopic = String.format("%s/%s", WebSocketConfig.TOPIC_FOR_JOB, job.getNodePath());
        super.push(jobTopic, job);
    }

    //TODO: because transaction not found job, add READ_UNCOMMITTED also no use
    protected void push(Job job) {
        Job jobConsist;
        try {
            jobConsist = jobService.find(job.getId());
        } catch (NotFoundException e) {
            jobConsist = job;
        }
        jobConsist.setStatus(job.getStatus());
        String jobTopic = String.format("%s/%s", WebSocketConfig.TOPIC_FOR_JOB, jobConsist.getNodePath());
        super.push(jobTopic, jobConsist);
    }
}
