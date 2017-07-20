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

import com.flow.platform.cc.context.ContextEvent;
import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cmd queue consumer to handle cmd from RabbitMQ
 *
 * @author gy@fir.im
 */
@Component(value = "cmdQueueConsumer")
public class CmdQueueConsumer implements ContextEvent {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    private final static int RETRY_QUEUE_PRIORITY = 255;

    @Value("${mq.exchange.name}")
    private String cmdExchangeName;

    @Autowired
    private Channel cmdSendChannel;

    @Autowired
    private Channel cmdConsumeChannel;

    @Autowired
    private String cmdConsumeQueue;

    @Autowired
    private CmdService cmdService;

    @Override
    public void start() {
        try {
            createConsume();
        } catch (IOException e) {
            LOGGER.error("Error when create consume of cmd queue", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (cmdConsumeChannel.isOpen()) {
            try {
                cmdConsumeChannel.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private void createConsume() throws IOException {
        cmdConsumeChannel.basicConsume(cmdConsumeQueue, false, new DefaultConsumer(cmdConsumeChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                Envelope envelope,
                AMQP.BasicProperties properties,
                byte[] body) throws IOException {

                CmdQueueItem item = null;
                try {
                    item = CmdQueueItem.parse(body, CmdQueueItem.class);
                    if (item == null) {
                        throw new IllegalParameterException("");
                    }

                    if (item.getCmdId() == null) {
                        throw new IllegalParameterException("");
                    }

                    LOGGER.trace("Receive cmd id '%s' from queue", item);
                } catch (Throwable e) {
                    LOGGER.error("Illegal data format in cmd queue", null);
                    return;
                }

                try {
                    cmdService.send(item.getCmdId(), false);
                } catch (IllegalParameterException e) {
                    LOGGER.warn("Illegal cmd id: %s", e.getMessage());
                } catch (IllegalStatusException e) {
                    LOGGER.warn("Illegal cmd status: %s", e.getMessage());
                } catch (AgentErr.NotAvailableException | ZkException.NotExitException e) {
                    if (item.getRetry() > 0) {
                        cmdService.resetStatus(item.getCmdId());
                        resend(item);
                    }
                } catch (Throwable e) {
                    LOGGER.error("Unexpected exception", e);
                } finally {
                    long deliveryTag = envelope.getDeliveryTag();
                    cmdConsumeChannel.basicAck(deliveryTag, false);
                }
            }
        });
    }

    private void resend(final CmdQueueItem item) {
        try {
            Thread.sleep(1000); // wait 1 seconds and enqueue again with priority
        } catch (InterruptedException ignore) {
            // do nothing
        }

        item.setPriority(RETRY_QUEUE_PRIORITY);
        item.setRetry(item.getRetry() - 1);

        try {
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .priority(item.getPriority())
                .build();

            // reset cmd status
            cmdSendChannel.basicPublish(cmdExchangeName, "", properties, item.toBytes());
            LOGGER.trace("Re-enqueue item %s", item);
        } catch (IOException e) {
            LOGGER.warn(String.format("Cmd %s re-enqueue fail, retry %s", item));
            if (item.getRetry() > 0) {
                resend(item);
            }
        }
    }
}