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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Log4j2
@Getter
public abstract class RabbitOperation implements AutoCloseable {

    private final static int DefaultExecutorQueueSize = 1000;

    protected final Connection conn;

    protected final Channel channel;

    protected final Integer concurrency;

    protected final String name;

    protected final ThreadPoolTaskExecutor executor;

    // key as queue name, value as instance
    protected final ConcurrentHashMap<String, QueueConsumer> consumers = new ConcurrentHashMap<>();

    public RabbitOperation(Connection conn, Integer concurrency, String name) throws IOException {
        this.conn = conn;
        this.concurrency = concurrency;
        this.name = name;
        this.channel = conn.createChannel();
        this.channel.basicQos(0, concurrency, false);
        this.executor = ThreadHelper.createTaskExecutor(concurrency, concurrency, DefaultExecutorQueueSize, name + "-");
    }

    public String declare(String queue, boolean durable) throws IOException {
        return this.channel.queueDeclare(queue, durable, false, false, null).getQueue();
    }

    public String declare(String queue, boolean durable, Integer maxPriority, String dlExName) throws IOException {
        Map<String, Object> props = new HashMap<>(1);
        props.put("x-max-priority", maxPriority);
        props.put("x-dead-letter-exchange", dlExName);
        props.put("x-dead-letter-routing-key", QueueConfig.JobDlRoutingKey);

        return this.channel.queueDeclare(queue, durable, false, false, props).getQueue();
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
                    .expiration(Long.toString(expireInSecond * 1000))
                    .build();

            this.channel.basicPublish(StringHelper.EMPTY, routingKey, props, body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public QueueConsumer getConsumer(String queue) {
        return consumers.get(queue);
    }

    public QueueConsumer createConsumer(String queue, Function<Message, Boolean> consume) {
        QueueConsumer consumer = new QueueConsumer(queue, consume);
        consumers.put(queue, consumer);
        return consumer;
    }

    public boolean removeConsumer(String queue) {
        QueueConsumer consumer = consumers.remove(queue);

        if (Objects.isNull(consumer)) {
            return false;
        }

        return consumer.cancel();
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
        executor.shutdown();
    }

    public class QueueConsumer extends DefaultConsumer {

        private final String queue;

        private final Function<Message, Boolean> consume;

        QueueConsumer(String queue, Function<Message, Boolean> consume) {
            super(channel);
            this.queue = queue;
            this.consume = consume;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            consume(body, envelope);
        }

        public void consume(byte[] body, Envelope envelope) {
            executor.execute(() -> {
                Boolean ignoreForNow = consume.apply(new Message(getChannel(), body, envelope));
            });
        }

        public String start(boolean autoAck) {
            try {
                String tag = getChannel().basicConsume(queue, autoAck, this);
                log.info("[Consumer STARTED] queue {} with tag {}", queue, tag);
                return tag;
            } catch (IOException e) {
                log.warn(e.getMessage());
                return null;
            }
        }

        boolean cancel() {
            try {
                if (Objects.isNull(getConsumerTag())) {
                    return true; // not started
                }

                consume.apply(Message.STOP_SIGN);
                getChannel().basicCancel(getConsumerTag());
                log.info("[Consumer STOP] queue {} with tag {}", queue, getConsumerTag());
                return true;
            } catch (IOException e) {
                log.warn(e.getMessage());
                return false;
            }
        }
    }

    @Getter
    public static class Message {

        public static final Message STOP_SIGN = new Message(null, new byte[0], null);

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
