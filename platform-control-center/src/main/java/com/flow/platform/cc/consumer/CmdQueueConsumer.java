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

import com.flow.platform.cc.config.QueueConfig;
import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdDispatchService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.QueueListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkException;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.PostConstruct;
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
@Component
public class CmdQueueConsumer implements QueueListener<Message> {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    @Value("${queue.cmd.idle_agent.period}")
    private Integer idleAgentPeriod; // period for check idle agent in seconds

    @Value("${queue.cmd.idle_agent.timeout}")
    private Integer idleAgentTimeout; // timeout if no idle agent in seconds

    @Autowired
    private CmdService cmdService;

    @Autowired
    private CmdDispatchService cmdDispatchService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private PlatformQueue<Message> cmdQueue;

    @PostConstruct
    public void init() {
        cmdQueue.register(this);
    }

    @Override
    public void onQueueItem(Message message) {
        CmdQueueItem item = CmdQueueItem.parse(message.getBody(), CmdQueueItem.class);
        String cmdId = item.getCmdId();
        LOGGER.trace("Receive a cmd queue item: %s", item);

        try {
            cmdDispatchService.dispatch(cmdId, false);
        } catch (IllegalParameterException e) {
            LOGGER.warn("Illegal cmd id: %s", e.getMessage());
        } catch (IllegalStatusException e) {
            LOGGER.warn("Illegal cmd status: %s", e.getMessage());
        } catch (AgentErr.NotAvailableException | AgentErr.NotFoundException | ZkException.NotExitException e) {
            if (item.getRetry() <= 0) {
                return;
            }

            boolean isTimeout = waitForIdleAgent(cmdId, idleAgentPeriod, idleAgentTimeout);
            if (isTimeout) {
                LOGGER.trace("wait for idle agent time out %s seconds for cmd %s", idleAgentTimeout, cmdId);
            }

            // reset cmd status to pending, record num of retry
            int retry = item.getRetry() - 1;
            Cmd cmd = cmdService.find(cmdId);
            if (cmd.getStatus() == CmdStatus.STOPPED || cmd.getStatus() == CmdStatus.KILLED) {
                return;
            }

            cmd.setStatus(CmdStatus.PENDING);
            cmd.setRetry(retry);
            cmdService.save(cmd);

            // re-enqueue
            resend(item, retry);

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

    /**
     * Re-enqueue cmd and return num of retry
     */
    private void resend(final CmdQueueItem item, final int retry) {
        if (retry <= 0) {
            return;
        }

        item.setPriority(QueueConfig.CMD_QUEUE_DEFAULT_PRIORITY);
        item.setRetry(retry);

        try {
            Thread.sleep(1000); // wait 1 seconds and enqueue again with priority
        } catch (InterruptedException ignore) {
            // do nothing
        }

        // reset cmd status
        MessageProperties properties = new MessageProperties();
        properties.setPriority(item.getPriority());
        Message message = new Message(item.toBytes(), properties);
        cmdQueue.enqueue(message);
        LOGGER.trace("Re-enqueue item %s", item);
    }
}