/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.rabbit;

import com.flowci.core.common.config.QueueConfig;
import com.flowci.util.StringHelper;
import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Log4j2
@Getter
public class RabbitOperations implements AutoCloseable {

    private final static int ExecutorQueueSize = 1000;

    private final Connection conn;

    private final Channel channel;

    private final Integer concurrency;

    // key as queue name, value as instance
    private final ConcurrentHashMap<String, QueueConsumer> consumers = new ConcurrentHashMap<>();

    public RabbitOperations(Connection conn, Integer concurrency) throws IOException {
        this.conn = conn;
        this.concurrency = concurrency;
        this.channel = conn.createChannel();
        this.channel.basicQos(1);
    }

    public void declareExchangeAndBind(String exchange, BuiltinExchangeType type, String queue, String routingKey) throws IOException {
        channel.exchangeDeclare(exchange, type);
        channel.queueBind(queue, exchange, routingKey);
    }

    public void declareExchangeAndBind(String exchange,
                                       BuiltinExchangeType type,
                                       boolean durable,
                                       boolean autoDelete,
                                       Map<String, Object> args,
                                       String queue,
                                       String routingKey) throws IOException {
        channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, durable, autoDelete, args);
        channel.queueBind(queue, exchange, routingKey);
    }

    public void declare(String queue, boolean durable) throws IOException {
        this.channel.queueDeclare(queue, durable, false, false, null);
    }

    public void declare(String queue, boolean durable, Integer maxPriority, String dlExName) throws IOException {
        Map<String, Object> props = new HashMap<>(3);
        props.put("x-max-priority", maxPriority);
        props.put("x-dead-letter-exchange", dlExName);
        props.put("x-dead-letter-routing-key", QueueConfig.JobDlRoutingKey);
        this.channel.queueDeclare(queue, durable, false, false, props);
    }

    public boolean delete(String queue) {
        try {
            this.channel.queueDelete(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean purge(String queue) {
        try {
            this.channel.queuePurge(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange
     */
    public boolean send(String routingKey, byte[] body) {
        try {
            this.channel.basicPublish(StringHelper.EMPTY, routingKey, null, body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange and priority
     */
    public boolean send(String routingKey, byte[] body, Integer priority, Long expireInSecond) {
        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .priority(priority)
                    .expiration("2000")
                    .build();

            this.channel.basicPublish(StringHelper.EMPTY, routingKey, props, body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void startConsumer(String queue, boolean autoAck, Function<Message, Boolean> onMessage) {
        QueueConsumer consumer = consumers.computeIfAbsent(queue, s -> new QueueConsumer(queue, onMessage));
        consumer.start(autoAck);
    }

    public void removeConsumer(String queue) {
        QueueConsumer consumer = consumers.remove(queue);
        if (consumer != null) {
            consumer.cancel();
        }
    }

    /**
     * It will be called when spring context stop
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        consumers.forEach((s, queueConsumer) -> queueConsumer.cancel());
        channel.close();
    }

    private class QueueConsumer extends DefaultConsumer {

        private final String queue;

        private final Function<Message, Boolean> onMessageFunc;

        QueueConsumer(String queue, Function<Message, Boolean> onMessageFunc) {
            super(channel);
            this.queue = queue;
            this.onMessageFunc = onMessageFunc;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            onMessageFunc.apply(new Message(getChannel(), body, envelope));
        }

        void start(boolean autoAck) {
            try {
                String tag = getChannel().basicConsume(queue, autoAck, this);
                log.info("[Consumer STARTED] queue {} with tag {}", queue, tag);
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }

        void cancel() {
            try {
                if (Objects.isNull(getConsumerTag())) {
                    return; // not started
                }

                getChannel().basicCancel(getConsumerTag());
                log.info("[Consumer STOP] queue {} with tag {}", queue, getConsumerTag());
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
    }

    @Getter
    public static class Message {

        private final Channel channel;

        private final byte[] body;

        private final Envelope envelope;

        public Message(Channel channel, byte[] body, Envelope envelope) {
            this.channel = channel;
            this.body = body;
            this.envelope = envelope;
        }

        public boolean sendAck() {
            try {
                getChannel().basicAck(envelope.getDeliveryTag(), false);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
