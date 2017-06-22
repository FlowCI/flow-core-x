package com.flow.platform.cc.consumer;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.Logger;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Consume cmd from rabbit mq
 *
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
@Component(value = "cmdQueueConsumer")
public class CmdQueueConsumer {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    private final static long MAX_CMD_INQUEUE_TIME = 60; // in seconds
    private final static int RETRY_QUEUE_PRIORITY = 5;
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

    @Autowired
    private Executor taskExecutor;

    @PostConstruct
    public void init() throws IOException {
        createConsume();
    }

    private void createConsume() throws IOException {
        cmdConsumeChannel.basicConsume(cmdConsumeQueue, true, new DefaultConsumer(cmdConsumeChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // convert byte to CmdBase
                CmdBase inputCmd;
                try {
                    inputCmd = CmdBase.parse(body, CmdBase.class);
                    LOGGER.trace("Receive a cmd from queue : %s", inputCmd);
                } catch (Throwable e) {
                    LOGGER.error("Unable to recognize cmd type", e);
                    return;
                }

                // send cmd and deal exception
                try {
                    cmdService.send(inputCmd);
                } catch (AgentErr.NotAvailableException e) {
                    resend(inputCmd, RETRY_QUEUE_PRIORITY, RETRY_TIMES);
                } catch (Throwable e) {
                    // unexpected err, throw e
                    LOGGER.error("Error when consume cmd from queue", e);
                } finally {
                    long deliveryTag = envelope.getDeliveryTag();
                    cmdConsumeChannel.basicAck(deliveryTag, false);
                }
            }
        });
    }

    private void resend(final CmdBase cmd, final int priority, final int retry) {
        taskExecutor.execute(() -> {
            try {
                Thread.sleep(1000); // wait 1 seconds and enqueue again with priority
            } catch (InterruptedException e) {
                // do nothing
            }

            try {
                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .priority(priority)
                        .build();

                LOGGER.trace("Re-enqueue for cmd %s with mq priority %s", cmd, priority);
                cmdSendChannel.basicPublish(cmdExchangeName, "", properties, cmd.toBytes());
            } catch (IOException e) {
                LOGGER.error(String.format("Cmd %s re-enqueue fail, retry %s", cmd, retry), e);
                if (retry > 0) {
                    resend(cmd, priority, retry - 1);
                }
            }
        });
    }
}
