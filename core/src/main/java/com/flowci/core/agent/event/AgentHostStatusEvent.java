package com.flowci.core.agent.event;

import com.flowci.core.agent.domain.AgentHost;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Agent status change event
 */
@Getter
public class AgentHostStatusEvent extends ApplicationEvent {

    private final AgentHost host;

    public AgentHostStatusEvent(Object source, AgentHost host) {
        super(source);
        this.host = host;
    }
}
