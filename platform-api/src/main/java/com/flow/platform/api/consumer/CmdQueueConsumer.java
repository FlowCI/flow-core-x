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

import com.flow.platform.api.domain.CmdQueueItem;
import com.flow.platform.api.service.JobService;
import com.flow.platform.core.consumer.QueueConsumerBase;
import com.flow.platform.util.Logger;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Component
public class CmdQueueConsumer extends QueueConsumerBase<CmdQueueItem> {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    @Autowired
    private BlockingQueue<CmdQueueItem> cmdBaseBlockingQueue;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private JobService jobService;

    @Override
    public ThreadPoolTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @Override
    public BlockingQueue<CmdQueueItem> getQueue() {
        return cmdBaseBlockingQueue;
    }

    @Override
    public void onQueueItem(CmdQueueItem item) {
        if (item == null) {
            return;
        }
        try {
            jobService.callback(item);
        }catch (Throwable throwable){
            LOGGER.traceMarker("onQueueItem", String.format("exception - %s", throwable));
        }
    }
}
