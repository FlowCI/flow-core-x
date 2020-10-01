package com.flowci.core.agent.event;

import com.flowci.core.common.event.BroadcastEvent;
import com.flowci.core.agent.domain.Agent;
import lombok.Getter;

/**
 * Has a idle agent
 */
@Getter
public class AgentIdleEvent extends BroadcastEvent {

    private Agent agent;

    public AgentIdleEvent() {
        super();
    }

    public AgentIdleEvent(Object source, Agent agent) {
        super(source);
        this.agent = agent;
    }
}
