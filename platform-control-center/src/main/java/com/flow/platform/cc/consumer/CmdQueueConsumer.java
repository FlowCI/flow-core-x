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

import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cmd queue consumer to handle cmd from RabbitMQ
 *
 * @author gy@fir.im
 */
@Component("cmdQueueConsumer")
public class CmdQueueConsumer {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    private final static int RETRY_QUEUE_PRIORITY = 10;

    @Value("${mq.queue.cmd.name}")
    private String cmdQueueName;

    @Value("${queue.cmd.idle_agent.period}")
    private Integer idleAgentPeriod; // period for check idle agent in seconds

    @Value("${queue.cmd.idle_agent.timeout}")
    private Integer idleAgentTimeout; // timeout if no idle agent in seconds

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RabbitTemplate cmdQueueTemplate;

    @RabbitListener(
        queues = {"${mq.queue.cmd.name}"},
        containerFactory = "cmdQueueContainerFactory",
        exclusive = true
    )
    public void onMessage(Message message) {
        CmdQueueItem item = CmdQueueItem.parse(message.getBody(), CmdQueueItem.class);
        String cmdId = item.getCmdId();
        LOGGER.trace("Receive a cmd queue item: %s", item);

        try {
            cmdService.send(cmdId, false);
        } catch (IllegalParameterException e) {
            LOGGER.warn("Illegal cmd id: %s", e.getMessage());
        } catch (IllegalStatusException e) {
            LOGGER.warn("Illegal cmd status: %s", e.getMessage());
        } catch (AgentErr.NotAvailableException | AgentErr.NotFoundException | ZkException.NotExitException e) {
            if (item.getRetry() > 0) {
                boolean isTimeout = waitForIdleAgent(cmdId, idleAgentPeriod, idleAgentTimeout);
                if (isTimeout) {
                    LOGGER.trace("wait for idle agent time out %s seconds for cmd %s", idleAgentTimeout, cmdId);
                }

                cmdService.resetStatus(cmdId);
                resend(item);
            }
        } catch (Throwable e) {
            LOGGER.error("Unexpected exception", e);
        }
    }

    /**
     * Block current thread and check idle agent
     *
     * @param cmdId cmd id
     * @param period check idle agent period in seconds
     * @param timeout timeout in seconds
     * @return is time out exit or not
     */
    private boolean waitForIdleAgent(String cmdId, int period, int timeout) {
        Instant now = Instant.now();

        while (true) {
            if (Duration.between(now, Instant.now()).toMillis() >= timeout * 1000) {
                return true;
            }

            try {
                Thread.sleep(period * 1000);
            } catch (InterruptedException ignore) {
            }

            String zone = cmdService.find(cmdId).getZoneName();
            int numOfIdle = agentService.findAvailable(zone).size();
            if (numOfIdle > 0) {
                LOGGER.trace("has %s idle agent", numOfIdle);
                return false;
            }
        }
    }

    private void resend(final CmdQueueItem item) {
        item.setPriority(RETRY_QUEUE_PRIORITY);
        item.setRetry(item.getRetry() - 1);

        if (item.getRetry() <= 0) {
            return;
        }

        try {
            Thread.sleep(1000); // wait 1 seconds and enqueue again with priority
        } catch (InterruptedException ignore) {
            // do nothing
        }

        // reset cmd status
        MessageProperties properties = new MessageProperties();
        properties.setPriority(item.getPriority());
        Message message = new Message(item.toBytes(), properties);
        cmdQueueTemplate.send("", cmdQueueName, message);
        LOGGER.trace("Re-enqueue item %s", item);
    }
}