package com.flowci.core.agent.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.socket.WebSocketSession;

@Getter
public abstract class EventFromClient extends ApplicationEvent {

    private final String token;

    private final WebSocketSession session;

    protected EventFromClient(Object source, String token, WebSocketSession session) {
        super(source);
        this.token = token;
        this.session = session;
    }
}
