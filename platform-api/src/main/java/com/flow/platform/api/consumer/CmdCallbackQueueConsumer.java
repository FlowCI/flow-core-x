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
import com.flow.platform.core.queue.QueueListener;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Component
public class CmdCallbackQueueConsumer implements QueueListener<CmdCallbackQueueItem> {

    private final static Logger LOGGER = new Logger(CmdCallbackQueueConsumer.class);

    @Autowired
    private PlatformQueue<CmdCallbackQueueItem> cmdCallbackQueue;

    @Autowired
    private JobService jobService;

    @PostConstruct
    public void init() {
        cmdCallbackQueue.register(this);
    }

    @Override
    public void onQueueItem(CmdCallbackQueueItem item) {
        if (item == null) {
            return;
        }
        try {
            jobService.callback(item);
        } catch (NotFoundException notFoundException) {
            // wait 1s re queue
            try {
                Thread.sleep(1000);
            } catch (Throwable throwable) {
            }

            jobService.enterQueue(item);

        } catch (Throwable throwable) {
            LOGGER.traceMarker("onQueueItem", String.format("exception - %s", throwable));
        }
    }
}
