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
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.util.StringHelper;
import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Log4j2
@Getter
public class RabbitOperations implements AutoCloseable {

    private final Connection conn;

    private final Channel channel;

    // key as queue name, value as consumer tag
    private final ConcurrentHashMap<String, String> consumers = new ConcurrentHashMap<>();

    private final ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(100, 100, 100, "rabbit-runner-");

    public RabbitOperations(Connection conn, int prefetch) throws IOException {
        this.conn = conn;
        this.channel = conn.createChannel();
        this.channel.basicQos(prefetch, false);
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
        channel.exchangeDeclare(exchange, type, durable, autoDelete, args);
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
    public boolean send(String routingKey, byte[] body, Integer priority, int expireInSecond) {
        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .priority(priority)
                    .expiration(Integer.toString(expireInSecond * 1000))
                    .build();

            this.channel.basicPublish(StringHelper.EMPTY, routingKey, props, body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void startConsumer(String queue, boolean autoAck, Function<Message, Boolean> onMessage) throws IOException {
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // run from executor that don't block rabbit event
                executor.execute(() -> {
                    log.debug("======= {} ======", new String(body));
                    onMessage.apply(new Message(getChannel(), body, envelope));
                });
            }
        };

        String tag = getChannel().basicConsume(queue, autoAck, consumer);
        consumers.put(queue, tag);
        log.info("[Consumer STARTED] queue {} with tag {}", queue, tag);
    }

    public void removeConsumer(String queue) {
        String consumerTag = consumers.remove(queue);
        if (consumerTag != null) {
            try {
                getChannel().basicCancel(consumerTag);
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }

    /**
     * It will be called when spring context stop
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        consumers.forEach((s, consumerTag) -> {
            try {
                getChannel().basicCancel(consumerTag);
            } catch (IOException ignore) {

            }
        });
        channel.close();
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
