package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.Logger;
import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Consume cmd from rabbit mq
 *
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
@Service
public class CmdQueueConsumer {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    private final static long MAX_CMD_INQUEUE_TIME = 60; // in seconds

    @Autowired
    private Channel cmdConsumeChannel;

    @Autowired
    private String cmdConsumeQueue;

    @Autowired
    private CmdService cmdService;

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

                try {
                    CmdBase cmd = CmdBase.parse(body, CmdBase.class);
                    LOGGER.trace("receive a cmd from queue : %s", cmd);

                    cmdService.send(cmd);
                } catch (AgentErr.NotAvailableException e) {
                    // TODO: no available exception, check max cmd from queue time and enqueue again

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
}
