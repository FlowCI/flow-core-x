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

package com.flow.platform.cc.consumer;

import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.queue.QueueListener;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * To update cmd status and agent status
 *
 * @author yang
 */
@Component
public class CmdStatusQueueConsumer implements QueueListener<PriorityMessage> {

    private final static Logger LOGGER = new Logger(CmdStatusQueueConsumer.class);

    @Autowired
    private PlatformQueue<PriorityMessage> cmdStatusQueue;

    @Autowired
    private CmdService cmdService;

    @PostConstruct
    public void init() {
        // register to cmd status queue
        cmdStatusQueue.register(this);
    }

    @Override
    public void onQueueItem(PriorityMessage item) {
        if (item == null) {
            return;
        }

        try {
            CmdStatusItem statusItem = CmdStatusItem.parse(item.getBody(), CmdStatusItem.class);
            LOGGER.debug(Thread.currentThread().getName() + " : " + item.toString());
            cmdService.updateStatus(statusItem, false);
        } catch (Throwable e) {
            LOGGER.error("Update cmd error:", e);
        }
    }
}
