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
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.Logger;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Log4j2
@Component
public class CmdCallbackQueueConsumer implements QueueListener<PriorityMessage> {

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
        if (Objects.isNull(message)) {
            return;
        }

        CmdCallbackQueueItem item = CmdCallbackQueueItem.parse(message.getBody(), CmdCallbackQueueItem.class);

        try {
            jobService.callback(item);
        } catch (NotFoundException notFoundException) {

            // detect retry times is reach the limit or not
            detectRetryTimes(item);

            // re-enqueue cmd callback if job not found since transaction problem
            reEnqueueJobCallback(item, REQUEUE_DELAY_TIME, message.getMessageProperties().getPriority());

        } catch (Throwable throwable) {
            log.trace("Exception on queue item: {}", throwable.getMessage());
        }
    }

    private void detectRetryTimes(CmdCallbackQueueItem item) {
        if (item.getRetryTimes() <= 0) {
            String message = "Retry times has reach the limitation since job '" + item.getJobId() + "'is not found";
            throw new FlowException(message);
        }
    }

    private void reEnqueueJobCallback(CmdCallbackQueueItem item, long wait, int priority) {
        // sleep seconds
        ThreadUtil.sleep(wait);

        // set retry times
        item.setRetryTimes(item.getRetryTimes() - 1);

        //priority inc 1
        jobService.enqueue(item, ++priority);
    }
}
