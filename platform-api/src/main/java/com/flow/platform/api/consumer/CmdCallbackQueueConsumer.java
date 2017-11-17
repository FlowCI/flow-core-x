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

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.queue.QueueListener;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Component
public class CmdCallbackQueueConsumer implements QueueListener<PriorityMessage> {

    private final static Logger LOGGER = new Logger(CmdCallbackQueueConsumer.class);

    // retry time set 5 times
    private final static int RETRY_TIMES = 5;

    // requeue 1 s
    private final static int REQUEUE_DELAY_TIME = 1000;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdCallbackQueue;

    @Autowired
    private JobService jobService;

    @PostConstruct
    public void init() {
        cmdCallbackQueue.register(this);
    }

    @Override
    public void onQueueItem(PriorityMessage message) {
        if (message == null) {
            return;
        }

        CmdCallbackQueueItem item = CmdCallbackQueueItem.parse(message.getBody(), CmdCallbackQueueItem.class);

        try {
            jobService.callback(item);
        } catch (NotFoundException notFoundException) {

            // detect retry times is reach the limit or not
            detectRetryTimes(item);

            // re-enqueue cmd callback if job not found since transaction problem
            reEnqueueJobCallback(item, REQUEUE_DELAY_TIME);

        } catch (Throwable throwable) {
            LOGGER.traceMarker("onQueueItem", String.format("exception - %s", throwable));
        }
    }

    private void detectRetryTimes(CmdCallbackQueueItem item) {
        if (item.getRetryTimes() > RETRY_TIMES) {
            throw new NotFoundException(String.format("retry times has reach the limit"));
        }
    }

    private void reEnqueueJobCallback(CmdCallbackQueueItem item, long wait) {
        try {
            Thread.sleep(wait);
        } catch (Throwable ignore) {
        }

        // set item priority priority inc
        item.setPriority(item.getPriority() + 1);

        // set retry times
        item.setRetryTimes(item.getRetryTimes() + 1);

        jobService.enterQueue(item);
    }
}
