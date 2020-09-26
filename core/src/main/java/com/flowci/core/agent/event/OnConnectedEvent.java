package com.flowci.core.agent.event;

import com.flowci.core.agent.domain.AgentInit;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class OnConnectedEvent extends EventFromClient {

    private final AgentInit init;

    public OnConnectedEvent(Object source, String token, WebSocketSession session, AgentInit init) {
        super(source, token, session);
        this.init = init;
    }
}
