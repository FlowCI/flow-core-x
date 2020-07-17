package com.flowci.core.common.rabbit;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import lombok.Getter;

import java.io.IOException;
import java.util.function.Function;

public class QueueOperations extends RabbitOperations {

    @Getter
    private final String queue;

    public QueueOperations(Connection conn, Integer concurrency, String queue) throws IOException {
        super(conn, concurrency);
        this.queue = queue;
    }

    public void declare(boolean durable) throws IOException {
        this.getChannel().queueDeclare(queue, durable, false, false, null);
    }

    public void declareExchangeAndBind(String exchange, BuiltinExchangeType type, String routingKey) throws IOException {
        super.declareExchangeAndBind(exchange, type, queue, routingKey);
    }

    public void startConsumer(boolean autoAck, OnMessage onMessage) throws IOException {
        super.startConsumer(queue, autoAck, onMessage);
    }

    public void purge() {
        super.purge(queue);
    }
}
