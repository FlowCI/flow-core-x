package com.flowci.core.agent.event;

import org.springframework.web.socket.WebSocketSession;

public class OnDisconnectedEvent extends EventFromAgent {

    public OnDisconnectedEvent(Object source, String token, WebSocketSession session) {
        super(source, token, session);
    }
}
