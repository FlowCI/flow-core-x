package com.flowci.core.agent.event;

import com.flowci.domain.Agent;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class OnStatusEvent extends EventFromClient {

    private final Agent.Status status;

    public OnStatusEvent(Object source, String token, WebSocketSession session, Agent.Status status) {
        super(source, token, session);
        this.status = status;
    }
}
