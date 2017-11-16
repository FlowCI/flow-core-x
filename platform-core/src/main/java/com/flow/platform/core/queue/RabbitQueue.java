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

package com.flow.platform.core.queue;

import com.flow.platform.util.Logger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class RabbitQueue extends PlatformQueue<PriorityMessage> {

    private final static Logger LOGGER = new Logger(RabbitQueue.class);

    private final static int DEFAULT_CONCURRENCY = 1;

    private final String host;

    private final String name;

    private final int maxPriority;

    private RabbitTemplate template;

    private SimpleMessageListenerContainer container;

    private volatile AtomicInteger size = new AtomicInteger(0);

    public RabbitQueue(ThreadPoolTaskExecutor executor, String host, int maxSize, int maxPriority, String queueName) {
        super(executor, maxSize, queueName);
        this.host = host;
        this.name = queueName;
        this.maxPriority = maxPriority;

        try {
            initRabbitMQ();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to init rabbit mq for host: " + host);
        }
    }

    @Override
    public void start() {
        container.start();
        LOGGER.trace("RabbitMQ ready to process");
    }

    @Override
    public void stop() {
        cleanListener();
        container.stop();
    }

    @Override
    public void pause() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    @Override
    public void resume() {
        if (!container.isRunning()) {
            container.start();
        }
    }

    @Override
    public void enqueue(PriorityMessage item) {
        template.send("", name, item);
        size.incrementAndGet();
    }

    @Override
    public void clean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }

    @Override
    public int size() {
        return size.get();
    }

    private void initRabbitMQ() throws URISyntaxException {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(new URI(host));

        Map<String, Object> cmdQueueArgs = new HashMap<>();
        cmdQueueArgs.put("x-max-length", maxSize);
        cmdQueueArgs.put("x-max-priority", maxPriority);
        Queue cmdQueue = new Queue(name, true, false, false, cmdQueueArgs);

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareQueue(cmdQueue);

        // setup listener container factory
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(DEFAULT_CONCURRENCY);
        factory.setMaxConcurrentConsumers(DEFAULT_CONCURRENCY);
        factory.setTaskExecutor(executor);
        factory.setAutoStartup(false);

        // setup rabbit template
        template = new RabbitTemplate(connectionFactory);
        template.setQueue(name);

        // setup container
        SimpleRabbitListenerEndpoint simpleEndpoint = new SimpleRabbitListenerEndpoint();
        simpleEndpoint.setMessageListener(new RabbitMessageListener());

        container = factory.createListenerContainer(simpleEndpoint);
        container.setQueueNames(name);
        LOGGER.trace("RabbitMQ initialized on '%s' with queue name '%s'", host, name);
    }

    private class RabbitMessageListener implements MessageListener {

        @Override
        public void onMessage(Message message) {
            size.decrementAndGet();
            for (QueueListener<PriorityMessage> listener : listeners) {
                listener.onQueueItem(new PriorityMessage(message));
            }
        }
    }
}
