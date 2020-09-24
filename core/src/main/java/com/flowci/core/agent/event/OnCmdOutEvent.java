package com.flowci.core.agent.event;

import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

public class OnCmdOutEvent extends EventFromClient {

    @Getter
    private final byte[] raw;

    public OnCmdOutEvent(Object source, String token, WebSocketSession session, byte[] raw) {
        super(source, token, session);
        this.raw = raw;
    }
}
