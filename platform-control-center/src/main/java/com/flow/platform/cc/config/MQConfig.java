package com.flow.platform.cc.config;

import com.flow.platform.util.Logger;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
@Configuration
public class MQConfig {

    private final static Logger LOGGER = new Logger(MQConfig.class);

    @Value("${mq.host}")
    private String host;

    @Value("${mq.exchange.name}")
    private String cmdExchangeName;

    // MQ connection
    private Connection rabbitMqConn;

    // define send and consume channel for cmd
    private Channel cmdSendChannel;
    private Channel cmdConsumeChannel;
    private String cmdConsumeQueue;

    @PostConstruct
    public void init() {
        try {
            rabbitMqConn = createChannel(host);
            LOGGER.trace("RabbitMQ connection created : %s", rabbitMqConn.toString());

            cmdSendChannel = rabbitMqConn.createChannel();
            cmdSendChannel.exchangeDeclare(cmdExchangeName, BuiltinExchangeType.FANOUT);
            LOGGER.trace("RabbitMQ cmd send channel created : %s", cmdSendChannel.toString());

            cmdConsumeChannel = rabbitMqConn.createChannel();
            cmdConsumeQueue = cmdConsumeChannel.queueDeclare().getQueue();
            cmdConsumeChannel.queueBind(cmdConsumeQueue, cmdExchangeName, "");
            LOGGER.trace("RabbitMQ cmd consume channel created : %s", cmdConsumeQueue);

        } catch (Throwable e) {
            LOGGER.error(String.format("Fail to init MQ for host: %s with exchange name: %s", host, cmdExchangeName), e);
        }
    }

    @Bean
    public Channel cmdSendChannel() {
        return cmdSendChannel;
    }

    @Bean
    public Channel cmdConsumeChannel() {
        return cmdConsumeChannel;
    }

    @Bean
    public String cmdConsumeQueue() {
        return cmdConsumeQueue;
    }

    @PreDestroy
    public void preDestroy() {
        closeChannel(cmdSendChannel);
        closeChannel(cmdConsumeChannel);
        closeConnection(rabbitMqConn);
    }

    private static void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Throwable e) {
            LOGGER.error("Err on close channel", e);
        }
    }

    private static void closeConnection(Connection conn) {
        try {
            if (conn != null && conn.isOpen()) {
                conn.close();
            }
        } catch (Throwable e) {
            LOGGER.error("Err on close connection", e);
        }
    }

    private static Connection createChannel(String host) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        return factory.newConnection();
    }
}
