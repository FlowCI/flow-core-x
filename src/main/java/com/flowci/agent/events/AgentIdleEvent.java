package com.flowci.agent.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgentIdleEvent extends ApplicationEvent {

    private final Long agentId;

    public AgentIdleEvent(Object source, Long agentId) {
        super(source);
        this.agentId = agentId;
    }
}
