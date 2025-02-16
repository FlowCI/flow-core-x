package com.flowci.common.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.model.RabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.flowci.common.config.AmqpConfig.EXCHANGE_NAME;

@Slf4j
@Component
public class PublishRabbitEventImpl implements PublishRabbitEvent {

    private final static Long WAIT_TIMEOUT = 5000L; // 5 seconds

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PublishRabbitEventImpl(@Autowired(required = false) RabbitTemplate rabbitTemplate,
                                  ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void invoke(RabbitEvent event) {
        if (rabbitTemplate == null) {
            log.debug("rabbitmq is not configured");
            return;
        }

        try {
            var eventJson = objectMapper.writeValueAsString(event);
            rabbitTemplate.send(EXCHANGE_NAME, event.getRoutingKey(), new Message(eventJson.getBytes()));

            if (rabbitTemplate.waitForConfirms(WAIT_TIMEOUT)) {
                log.info("Event {} published successfully", eventJson);
            }
        } catch (JsonProcessingException e) {
            log.error("unable parse event to json", e);
        }
    }
}
