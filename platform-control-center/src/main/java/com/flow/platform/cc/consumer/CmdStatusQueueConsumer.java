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
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * To update cmd status and agent status
 *
 * @author yang
 */
@Log4j2
@Component
public class CmdStatusQueueConsumer implements QueueListener<PriorityMessage> {

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
            log.debug(Thread.currentThread().getName() + " : " + item.toString());
            cmdService.updateStatus(statusItem, false);
        } catch (Throwable e) {
            log.error("Update cmd error:", e);
        }
    }
}
