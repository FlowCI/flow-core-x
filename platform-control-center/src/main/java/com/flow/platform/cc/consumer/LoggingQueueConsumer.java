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

import com.flow.platform.util.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component("loggingQueueConsumer")
public class LoggingQueueConsumer {

    private final static Logger LOGGER = new Logger(LoggingQueueConsumer.class);

    @Autowired
    private SimpMessagingTemplate template;

    @RabbitListener(queues = {"${mq.queue.logging.name}"}, containerFactory = "loggingQueueContainerFactory")
    public void onLogging(Message message) {
        String log = new String(message.getBody());
        LOGGER.debug("Receive log: " + log);
    }
}
