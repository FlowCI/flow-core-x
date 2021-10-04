package com.flowci.core.agent.event;

import com.flowci.core.common.event.AsyncEvent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.socket.WebSocketSession;

@Getter
public abstract class EventFromAgent extends ApplicationEvent implements AsyncEvent {

    private final String token;

    private final WebSocketSession session;

    protected EventFromAgent(Object source, String token, WebSocketSession session) {
        super(source);
        this.token = token;
        this.session = session;
    }
}
