package com.flowci.core.agent.event;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentInit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class OnConnectedEvent extends EventFromAgent {

    private final AgentInit init;

    private Agent agent;

    private final boolean toIdleQueue;

    public OnConnectedEvent(Object source, String token, WebSocketSession session, AgentInit init, boolean toIdleQueue) {
        super(source, token, session);
        this.init = init;
        this.toIdleQueue = toIdleQueue;
    }
}
