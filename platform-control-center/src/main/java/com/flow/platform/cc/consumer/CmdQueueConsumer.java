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
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.util.Logger;
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
    private final static int RETRY_TIMES = 5;

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

                // convert byte to CmdBase
                CmdInfo inputCmd;
                try {
                    inputCmd = CmdInfo.parse(body, CmdInfo.class);
                    LOGGER.trace("Receive a cmd from queue : %s", inputCmd);
                } catch (Throwable e) {
                    LOGGER.error("Unable to recognize cmd type", e);
                    return;
                }

                // send cmd and deal exception
                try {
                    Cmd cmd = cmdService.send(inputCmd);
                    LOGGER.trace("Cmd been sent to agent: %s", cmd);
                } catch (AgentErr.NotAvailableException e) {
                    resend(inputCmd, RETRY_QUEUE_PRIORITY, RETRY_TIMES);
                } catch (Throwable e) {
                    // unexpected err, throw e
                    inputCmd.setStatus(CmdStatus.EXCEPTION);
                    cmdService.webhookCallback(inputCmd);
                    LOGGER.error("Error when consume cmd from queue", e);
                } finally {
                    long deliveryTag = envelope.getDeliveryTag();
                    cmdConsumeChannel.basicAck(deliveryTag, false);
                }
            }
        });
    }

    private void resend(final CmdBase cmd, final int priority, final int retry) {
        try {
            Thread.sleep(1000); // wait 1 seconds and enqueue again with priority
        } catch (InterruptedException e) {
            // do nothing
        }

        try {
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .priority(priority)
                .build();

            // reset cmd status
            cmd.setStatus(CmdStatus.PENDING);
            cmdSendChannel.basicPublish(cmdExchangeName, "", properties, cmd.toBytes());
            LOGGER.trace("Re-enqueue for cmd %s with mq priority %s", cmd, priority);
        } catch (IOException e) {
            LOGGER.warn(String.format("Cmd %s re-enqueue fail, retry %s", cmd, retry));
            if (retry > 0) {
                resend(cmd, priority, retry - 1);
            }
        }
    }
}
