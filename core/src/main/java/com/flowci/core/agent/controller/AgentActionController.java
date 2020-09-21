package com.flowci.core.agent.controller;

import com.flowci.domain.Agent;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class AgentActionController extends BinaryWebSocketHandler {

    private final static String EventConnect = "connect___";

    private final static String EventStateChange = "status____";

    private final static String EventShellLog = "slog______";

    private final static String EventTTYLog = "tlog______";

    private final Map<Agent, WebSocketSession> agentSessionStore = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] bytes = message.getPayload().array();

        if (bytes.length < 10) {
            log.warn("invalid binary message");
            return;
        }

        String event = new String(Arrays.copyOf(bytes, 10));
        if (EventConnect.equals(event)) {
            return;
        }

        if (EventStateChange.equals(event)) {
            return;
        }

        if (EventShellLog.equals(event)) {
            return;
        }

        if (EventTTYLog.equals(event)) {
            return;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}
