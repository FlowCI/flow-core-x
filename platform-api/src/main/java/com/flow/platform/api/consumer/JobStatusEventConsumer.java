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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.events.JobStatusChangeEvent;
import com.flow.platform.api.service.MessageService;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;

/**
 * To handle JobStatusChangeEvent and NodeResultStatusChangeEvent
 *
 * @author yang
 */
public class JobStatusEventConsumer extends JobEventPushHandler implements ApplicationListener<JobStatusChangeEvent> {

    private final static Logger LOGGER = new Logger(JobStatusEventConsumer.class);

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private MessageService messageService;

    @Override
    public void onApplicationEvent(JobStatusChangeEvent event) {
        LOGGER.debug("Job %s status change event from %s to %s", event.getJobId(), event.getFrom(), event.getTo());

        push(event.getJobId());

        // async send failure email
        if (Job.FAILURE_STATUS.contains(event.getTo())) {
            sendFailEmail(event.getJobId());
        }
    }

    private void sendFailEmail(BigInteger jobId) {
        taskExecutor.execute(() -> messageService.sendMessage(jobId));
    }
}
