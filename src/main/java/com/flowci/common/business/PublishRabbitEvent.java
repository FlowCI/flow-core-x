package com.flowci.common.business;

import com.flowci.common.model.RabbitEvent;

public interface PublishRabbitEvent {
    void invoke(RabbitEvent event);
}
