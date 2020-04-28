package com.flowci.core.job.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StopJobConsumerEvent extends ApplicationEvent {

    private final String flowId;

    public StopJobConsumerEvent(Object source, String flowId) {
        super(source);
        this.flowId = flowId;
    }
}

