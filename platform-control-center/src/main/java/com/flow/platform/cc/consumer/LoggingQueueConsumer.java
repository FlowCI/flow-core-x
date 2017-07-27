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

    private final static int MIN_LENGTH_LOG = 6;

    @Autowired
    private SimpMessagingTemplate template;

    @RabbitListener(queues = {"${mq.queue.logging.name}"}, containerFactory = "loggingQueueContainerFactory")
    public void onLogging(Message message) {
        String logItem = new String(message.getBody());
        LOGGER.debug("Receive logItem %s : %s", Thread.currentThread().getName(), logItem);

        if (logItem.length() < MIN_LENGTH_LOG) {
            return;
        }

        // parse log item "zone#agent#cmdId#content" and send to event "zone:agent"
        int zoneIndex = logItem.indexOf('#', 0);
        String zone = logItem.substring(0, zoneIndex);

        int agentIndex = logItem.indexOf('#', zoneIndex + 1);
        String agent = logItem.substring(zoneIndex + 1, agentIndex);

        int cmdIdIndex = logItem.indexOf('#', agentIndex + 1);
        String cmdId = logItem.substring(agentIndex + 1, cmdIdIndex);

        String content = logItem.substring(cmdIdIndex + 1);

        String event = String.format("/topic/%s:%s", zone, agent);
        template.convertAndSend(event, content);
    }
}
