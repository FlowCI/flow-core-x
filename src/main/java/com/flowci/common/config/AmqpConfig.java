package com.flowci.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.time.Duration;

@Configuration
@ConditionalOnProperty("spring.rabbitmq.host")
public class AmqpConfig {

    public final static String EXCHANGE_NAME = "flows.exchange";
//    public final static String BUILDS_QUEUE_NAME = "flows.builds.queue";
//    public final static String BUILDS_ROUTING_KEY = "builds";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRetryTemplate(new RetryTemplateBuilder()
                .fixedBackoff(Duration.ofSeconds(2L))
                .maxAttempts(3)
                .build());
        return rabbitTemplate;
    }

//    @Bean
//    public Declarables declarables() {
//        var exchange = new DirectExchange(EXCHANGE_NAME);
//        var queueForBuilds = QueueBuilder.durable(BUILDS_QUEUE_NAME)
//                .quorum()
//                .build();
//
//        return new Declarables(
//                exchange,
//                queueForBuilds,
//                BindingBuilder.bind(queueForBuilds).to(exchange).with(BUILDS_ROUTING_KEY)
//        );
//    }
}
